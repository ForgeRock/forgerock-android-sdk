/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.json.JSONException
import java.io.IOException
import java.security.*
import java.security.interfaces.RSAPublicKey

interface KeyAwareInterface {
    fun generateKeys(context: Context, challenge: String, authenticationType: DeviceBindingAuthenticationType): KeyPair?
    fun sign(keyPair: KeyPair, kid: String, challenge: String): String
}

data class KeyPair(
     val publicKey: RSAPublicKey,
     val privateKey: PrivateKey,
     var keyAlias: String
)

/**
 * Helper class to generate keypair, sign the challenge
 */
class KeyAware(private var userId: String? = null) : KeyAwareInterface {

    private val hashingAlgorithm = "SHA-256"
    private val keySize = 2048
    private val timeout = 60
    private val androidKeyStore = "AndroidKeyStore"
    private val encryptionBlockMode = KeyProperties.BLOCK_MODE_ECB
    private val encryptionPadding = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
    private val encryptionAlgorithm = KeyProperties.KEY_ALGORITHM_RSA
    private val signaturePadding = KeyProperties.SIGNATURE_PADDING_RSA_PKCS1
    private val purpose = KeyProperties.PURPOSE_SIGN or
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT

    @Throws(
        NoSuchProviderException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        JSONException::class
    )
    override fun generateKeys(context: Context,
                              challenge: String,
                              authenticationType: DeviceBindingAuthenticationType): KeyPair? {
        return getKeyAlias()?.let {
            val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(
                encryptionAlgorithm, androidKeyStore
            )
            val builder = KeyGenParameterSpec.Builder(
                it,
                purpose
            ).setDigests(KeyProperties.DIGEST_SHA512)
                .setKeySize(keySize)
                .setSignaturePaddings(signaturePadding)
                .setBlockModes(encryptionBlockMode)
                .setEncryptionPaddings(encryptionPadding)

            if(authenticationType != DeviceBindingAuthenticationType.NONE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setUserAuthenticationParameters(timeout,
                        if(authenticationType == DeviceBindingAuthenticationType.BIOMETRIC_ONLY) KeyProperties.AUTH_BIOMETRIC_STRONG else
                        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
                } else {
                    builder.setUserAuthenticationValidityDurationSeconds(timeout)
                }
                builder.setUserAuthenticationRequired(true)
            }

            keyPairGenerator.initialize(builder.build())
            keyPairGenerator.generateKeyPair()

            val keyStore = getKeyStore()
            val publicKey = keyStore.getCertificate(it).publicKey as? RSAPublicKey
            val privateKey = keyStore.getKey(it, null) as? PrivateKey

            when {
                publicKey == null || privateKey == null -> {
                    return null
                }
                else -> {
                    return KeyPair(publicKey, privateKey, it)
                }
            }
        }
    }


    @Throws(
        GeneralSecurityException::class,
        IOException::class,
        JSONException::class,
        JOSEException::class
    )
    override fun sign(keyPair: KeyPair, kid: String, challenge: String): String {
        val jwk: JWK = RSAKey.Builder(keyPair.publicKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(kid)
            .algorithm(JWSAlgorithm.RS512)
            .build()
        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS512)
                .keyID(kid)
                .jwk(jwk)
                .build(), JWTClaimsSet.Builder()
                .subject(userId)
                .claim("challenge", challenge)
                .build()
        )
        signedJWT.sign(RSASSASigner(keyPair.privateKey))
        return signedJWT.serialize()
    }

    private fun getKeyAlias(keyName: String? = userId): String? {
        return keyName?.let {
            getHash(it)
        }
    }

    /**
     * Creates centralised SecretKey using the KeyStore
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance(androidKeyStore)
        keyStore.load(null)
        return keyStore
    }

    internal fun getHash(value: String): String? {
        return try {
            val digest: MessageDigest = MessageDigest.getInstance(hashingAlgorithm)
            val hash: ByteArray? = digest.digest(value.toByteArray())
            Base64.encodeToString(hash, Base64.DEFAULT)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
        catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}