/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.content.Context
import android.os.Build
import android.os.Parcelable
import com.nimbusds.jose.JWSAlgorithm.*
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.util.Base64
import com.nimbusds.jwt.JWTClaimNames
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import kotlinx.parcelize.Parcelize
import org.forgerock.android.auth.CryptoKey
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.callback.Attestation
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.*

private val TAG = DeviceAuthenticator::class.java.simpleName

private const val ANDROID_VERSION = "android-version"
private const val CHALLENGE = "challenge"
private const val PLATFORM = "platform"

/**
 * Device Authenticator Interface
 */
interface DeviceAuthenticator {

    /**
     * generate the public and private [KeyPair] with Challenge
     */
    suspend fun generateKeys(context: Context, attestation: Attestation): KeyPair

    /**
     * Authenticate the user to access the
     */
    suspend fun authenticate(context: Context): DeviceBindingStatus

    /**
     * Set the Authentication Prompt
     */
    fun prompt(prompt: Prompt) {
        //Do Nothing
    }

    /**
     * The JWS algorithm (alg) parameter.
     * Header Parameter identifies the cryptographic algorithm
     * used to secure the JWS.
     */
    fun getAlgorithm(): String {
        return "RS512"
    }

    /**
     * sign the challenge sent from the server and generate signed JWT
     * @param keyPair Public and private key
     * @param kid Generated kid from the Preference
     * @param userId userId received from server
     * @param challenge challenge received from server
     * @param customClaims A map of custom claims to be added to the jws payload
     */
    fun sign(context: Context,
             keyPair: KeyPair,
             signature: Signature?,
             kid: String,
             userId: String,
             challenge: String,
             expiration: Date,
             attestation: Attestation = Attestation.None): String {
        val builder = RSAKey.Builder(keyPair.publicKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(kid)
            .algorithm(parse(getAlgorithm()))
        if (attestation !is Attestation.None) {
            builder.x509CertChain(getCertificateChain(userId))
        }
        val jwk = builder.build();
        val signedJWT = SignedJWT(JWSHeader.Builder(parse(getAlgorithm()))
            .keyID(kid).jwk(jwk).build(),
            JWTClaimsSet.Builder().subject(userId)
                .issuer(context.packageName)
                .expirationTime(expiration)
                .issueTime(getIssueTime())
                .notBeforeTime(getNotBeforeTime())
                .claim(PLATFORM, "android")
                .claim(ANDROID_VERSION, Build.VERSION.SDK_INT)
                .claim(CHALLENGE, challenge).build())
        signature?.let {
            //Using CryptoObject
            Logger.info(TAG, "Use CryptObject signature for Signing")
            signedJWT.sign(RSASASignatureSigner(signature))
        } ?: run {
            Logger.info(TAG, "Use Private Key for Signing")
            signedJWT.sign(RSASSASigner(keyPair.privateKey))
        }

        return signedJWT.serialize()
    }

    private fun getCertificateChain(userId: String): List<Base64> {
        val chain = CryptoKey(userId).getCertificateChain()
        return chain.map {
            Base64.encode(it.encoded)
        }.toList()
    }

    /**
     * sign the challenge sent from the server and generate signed JWT
     * @param userKey User Information
     * @param challenge challenge received from server
     * @param customClaims A map of custom claims to be added to the jws payload
     */
    fun sign(context: Context,
             userKey: UserKey,
             privateKey: PrivateKey,
             signature: Signature?,
             challenge: String,
             expiration: Date,
             customClaims: Map<String, Any> = emptyMap()): String {
        val claimsSet = JWTClaimsSet.Builder().subject(userKey.userId)
            .issuer(context.packageName)
            .claim(CHALLENGE, challenge)
            .issueTime(getIssueTime())
            .notBeforeTime(getNotBeforeTime())
            .expirationTime(expiration)
        customClaims.forEach { (key, value) ->
            claimsSet.claim(key, value)
        }
        val signedJWT =
            SignedJWT(JWSHeader.Builder(parse(getAlgorithm()))
                .keyID(userKey.kid).build(),
                claimsSet.build())
        //Use provided signature to sign if available otherwise use private key
        signature?.let {
            //Using CryptoObject
            Logger.info(TAG, "Use CryptObject signature for Signing")
            signedJWT.sign(RSASASignatureSigner(signature))
        } ?: run {
            Logger.info(TAG, "Use Private Key for Signing")
            signedJWT.sign(RSASSASigner(privateKey))
        }

        return signedJWT.serialize()
    }

    /**
     * check if supported device binding
     */
    fun isSupported(context: Context, attestation: Attestation = Attestation.None): Boolean {
        return if (attestation !is Attestation.None) {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        } else {
            true
        }
    }

    fun type(): DeviceBindingAuthenticationType

    fun deleteKeys(context: Context)

    /**
     * Get the token signed issue time.
     * @return The issue time
     */
    fun getIssueTime(): Date {
        return Calendar.getInstance().time
    }

    /**
     * Get the token not before time.
     * @return The not before time
     */
    fun getNotBeforeTime(): Date {
        return Calendar.getInstance().time
    }

    /** Validate custom claims
     * @param  customClaims: A map of custom claims to be validated
     * @return Boolean value indicating whether the custom claims are valid or not
     */
    fun validateCustomClaims(customClaims: Map<String, Any>): Boolean {
        return customClaims.keys.intersect(registeredKeys).isEmpty()
    }

    companion object {
        val registeredKeys = listOf(
            JWTClaimNames.SUBJECT,
            JWTClaimNames.EXPIRATION_TIME,
            JWTClaimNames.ISSUED_AT,
            JWTClaimNames.NOT_BEFORE,
            JWTClaimNames.ISSUER,
            CHALLENGE
        )
    }

}

fun DeviceAuthenticator.initialize(userId: String, prompt: Prompt): DeviceAuthenticator {

    //Inject objects
    if (this is BiometricAuthenticator) {
        this.setBiometricHandler(BiometricBindingHandler(prompt.title,
            prompt.subtitle,
            prompt.description,
            deviceBindAuthenticationType = this.type()))
    }
    initialize(userId)
    this.prompt(prompt)
    return this
}

fun DeviceAuthenticator.initialize(userId: String): DeviceAuthenticator {
    //Inject objects
    if (this is CryptoAware) {
        this.setKey(CryptoKey(userId))
    }
    return this
}

@Parcelize
class Prompt(val title: String, val subtitle: String, var description: String) : Parcelable

/**
 * Create public and private keypair
 * @param publicKey The RSA Public key
 * @param privateKey The RSA Private key
 * @param keyAlias KeyAlias for
 */

data class KeyPair(val publicKey: RSAPublicKey, val privateKey: PrivateKey, var keyAlias: String)

