/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.security.keystore.KeyProperties
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.ERROR_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_HW_NOT_PRESENT
import androidx.biometric.BiometricPrompt.ERROR_HW_UNAVAILABLE
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT
import androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
import androidx.biometric.BiometricPrompt.ERROR_NO_BIOMETRICS
import androidx.biometric.BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt.ERROR_NO_SPACE
import androidx.biometric.BiometricPrompt.ERROR_TIMEOUT
import androidx.biometric.BiometricPrompt.ERROR_UNABLE_TO_PROCESS
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_VENDOR
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.forgerock.android.auth.CryptoKey
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Device Authenticator Interface
 */
interface DeviceAuthenticator {

    /**
     * generate the public and private [KeyPair]
     */
    fun generateKeys(context: Context, callback: (KeyPair) -> Unit)

    /**
     * Authenticate the user to access the
     * @param timeout Timeout for authentication
     * @param statusResult Listener for receiving authentication status
     */
    fun authenticate(context: Context,
                     timeout: Int,
                     statusResult: (DeviceBindingStatus<PrivateKey>) -> Unit)

    /**
     * sign the challenge sent from the server and generate signed JWT
     * @param keyPair Public and private key
     * @param kid Generated kid from the Preference
     * @param userId userId received from server
     * @param challenge challenge received from server
     */
    fun sign(keyPair: KeyPair,
             kid: String,
             userId: String,
             challenge: String,
             expiration: Date): String {
        val jwk: JWK = RSAKey.Builder(keyPair.publicKey).keyUse(KeyUse.SIGNATURE).keyID(kid)
            .algorithm(JWSAlgorithm.RS512).build()
        val signedJWT = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS512).keyID(kid).jwk(jwk).build(),
            JWTClaimsSet.Builder().subject(userId).expirationTime(expiration)
                .claim("challenge", challenge).build())
        signedJWT.sign(RSASSASigner(keyPair.privateKey))
        return signedJWT.serialize()
    }

    /**
     * sign the challenge sent from the server and generate signed JWT
     * @param userKey User Information
     * @param challenge challenge received from server
     */
    fun sign(userKey: UserKey,
             privateKey: PrivateKey,
             challenge: String,
             expiration: Date): String {
        val signedJWT = SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS512).keyID(userKey.kid).build(),
            JWTClaimsSet.Builder().subject(userKey.userId).claim("challenge", challenge)
                .expirationTime(expiration).build())
        signedJWT.sign(RSASSASigner(privateKey))
        return signedJWT.serialize()
    }

    /**
     * check biometric is supported
     */
    fun isSupported(context: Context): Boolean

    fun type(): DeviceBindingAuthenticationType

}

fun DeviceAuthenticator.initialize(userId: String,
                                   title: String,
                                   subtitle: String,
                                   description: String) {

    //Inject objects
    if (this is BiometricAuthenticator) {
        this.setBiometricHandler(BiometricBindingHandler(title,
            subtitle,
            description,
            deviceBindAuthenticationType = this.type()))
    }
    if (this is CryptoAware) {
        this.setKey(CryptoKey(userId))
    }
}

/**
 * Create public and private keypair
 * @param publicKey The RSA Public key
 * @param privateKey The RSA Private key
 * @param keyAlias KeyAlias for
 */

data class KeyPair(val publicKey: RSAPublicKey, val privateKey: PrivateKey, var keyAlias: String)

abstract class BiometricAuthenticator() : CryptoAware, DeviceAuthenticator {

    @VisibleForTesting
    internal var isApi30OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    internal lateinit var cryptoKey: CryptoKey
    internal lateinit var biometricInterface: BiometricHandler

    final override fun setKey(cryptoKey: CryptoKey) {
        this.cryptoKey = cryptoKey
    }

    fun setBiometricHandler(biometricHandler: BiometricHandler) {
        this.biometricInterface = biometricHandler
    }

    /**
     * Display biometric prompt for authentication type
     * @param timeout Timeout for biometric prompt
     * @param statusResult Listener for receiving Biometric changes
     */
    override fun authenticate(context: Context,
                              timeout: Int,
                              statusResult: (DeviceBindingStatus<PrivateKey>) -> Unit) {
        val startTime = Date().time
        val listener = object : AuthenticationCallback() {

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    ERROR_CANCELED, ERROR_USER_CANCELED, ERROR_NEGATIVE_BUTTON -> statusResult(Abort(
                        errString.toString(),
                        code = errorCode))
                    ERROR_TIMEOUT -> statusResult(Timeout(errString.toString(), code = errorCode))
                    ERROR_NO_BIOMETRICS, ERROR_NO_DEVICE_CREDENTIAL, ERROR_HW_NOT_PRESENT -> statusResult(
                        Unsupported(errString.toString(), code = errorCode))
                    ERROR_VENDOR -> statusResult(Unsupported(errString.toString(),
                        code = errorCode))
                    ERROR_LOCKOUT_PERMANENT, ERROR_LOCKOUT, ERROR_NO_SPACE, ERROR_HW_UNAVAILABLE, ERROR_UNABLE_TO_PROCESS -> statusResult(
                        UnAuthorize(errString.toString(), code = errorCode))
                    else -> {
                        statusResult(Unknown(errString.toString(), code = errorCode))
                    }
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val endTime = TimeUnit.MILLISECONDS.toSeconds(Date().time - startTime)
                if (endTime > (timeout.toLong())) {
                    statusResult(Timeout())
                } else {
                    statusResult(Success(cryptoKey.getPrivateKey()))
                }
            }

            override fun onAuthenticationFailed() {
                statusResult(UnAuthorize())
            }

        }
        biometricInterface.authenticate(listener)
    }
}

/**
 * Settings  for all the biometric authentication is configured
 */
open class BiometricOnly : BiometricAuthenticator() {


    /**
     * generate the public and private keypair
     */
    @SuppressLint("NewApi")
    override fun generateKeys(context: Context, callback: (KeyPair) -> Unit) {
        val builder = cryptoKey.keyBuilder()
        if (isApi30OrAbove) {
            builder.setUserAuthenticationParameters(cryptoKey.timeout,
                KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(cryptoKey.timeout)
        }
        builder.setUserAuthenticationRequired(true)
        val key = cryptoKey.createKeyPair(builder.build())
        val keyPair = KeyPair(key.public as RSAPublicKey, key.private, cryptoKey.keyAlias)
        callback(keyPair)
    }

    /**
     * check biometric is supported
     */
    override fun isSupported(context: Context): Boolean {
        return biometricInterface.isSupported(BIOMETRIC_STRONG, BIOMETRIC_WEAK)
    }

    final override fun type(): DeviceBindingAuthenticationType =
        DeviceBindingAuthenticationType.BIOMETRIC_ONLY


}

/**
 * Settings for all the biometric authentication and device credential is configured
 */
open class BiometricAndDeviceCredential() : BiometricAuthenticator() {

    /**
     * generate the public and private keypair
     */
    @SuppressLint("NewApi")
    override fun generateKeys(context: Context, callback: (KeyPair) -> Unit) {
        val builder = cryptoKey.keyBuilder()
        if (isApi30OrAbove) {
            builder.setUserAuthenticationParameters(cryptoKey.timeout,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(cryptoKey.timeout)
        }
        builder.setUserAuthenticationRequired(true)
        val key = cryptoKey.createKeyPair(builder.build())
        val keyPair = KeyPair(key.public as RSAPublicKey, key.private, cryptoKey.keyAlias)
        callback(keyPair)
    }

    /**
     * check biometric is supported
     */
    override fun isSupported(context: Context): Boolean {
        return biometricInterface.isSupported(BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
    }

    final override fun type(): DeviceBindingAuthenticationType =
        DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK


}

/**
 * Settings for all the none authentication is configured
 */
open class None() : CryptoAware, DeviceAuthenticator {

    private lateinit var cryptoKey: CryptoKey

    /**
     * generate the public and private keypair
     */
    override fun generateKeys(context: Context, callback: (KeyPair) -> Unit) {
        val builder = cryptoKey.keyBuilder()
        val key = cryptoKey.createKeyPair(builder.build())
        val keyPair = KeyPair(key.public as RSAPublicKey, key.private, cryptoKey.keyAlias)
        callback(keyPair)
    }

    /**
     * Default is true for None type
     */
    override fun isSupported(context: Context): Boolean = true

    final override fun type(): DeviceBindingAuthenticationType =
        DeviceBindingAuthenticationType.NONE

    /**
     * return success block for None type
     */
    override fun authenticate(context: Context,
                              timeout: Int,
                              statusResult: (DeviceBindingStatus<PrivateKey>) -> Unit) {
        statusResult(Success(cryptoKey.getPrivateKey()))
    }

    final override fun setKey(cryptoKey: CryptoKey) {
        this.cryptoKey = cryptoKey
    }

}