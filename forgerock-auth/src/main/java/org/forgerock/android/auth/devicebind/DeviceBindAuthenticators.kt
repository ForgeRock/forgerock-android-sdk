/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.annotation.SuppressLint
import android.os.Build
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager.Authenticators.*
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey


/**
 * Interface to override keypair keys, biometric display, sign
 */
interface Authenticator {
    /**
     * generate the public and private keypair
     */
    fun generateKeys(): KeyPair
    /**
     * Display biometric prompt for authentication type
     * @param timeout Timeout for biometric prompt
     * @param statusResult Listener for receiving Biometric changes
     */
    fun authenticate(timeout: Int,  statusResult: (DeviceBindingStatus) -> Unit)
    /**
     * sign the challenge sent from the server and generate signed JWT
     * @param keyPair Public and private key
     * @param kid Generated kid from the Preference
     * @param userId userId received from server
     * @param challenge challenge received from server
     */
    fun sign(keyPair: KeyPair, kid: String, userId: String, challenge: String): String {
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

    fun sign(privateKey: PrivateKey, user: UserKey, challenge: String): String {
        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS512)
                .keyID(user.kid)
                .build(), JWTClaimsSet.Builder()
                .subject(user.userId)
                .claim("challenge", challenge)
                .build()
        )
        signedJWT.sign(RSASSASigner(privateKey))
        return signedJWT.serialize()
    }
    /**
     * check biometric is supported
     */
    fun isSupported(): Boolean

    fun getPrivateKey(keyAlias: String, keyAware: KeyAware): PrivateKey? {
        return keyAware.getPrivateKey(keyAlias)
    }

}

/**
 * Create public and private keypair
 * @param publicKey The RSA Public key
 * @param privateKey The RSA Private key
 * @param keyAlias KeyAlias for
 */

data class KeyPair(
    val publicKey: RSAPublicKey,
    val privateKey: PrivateKey,
    var keyAlias: String
)

/**
 * Settings  for all the biometric authentication is configured
 */
internal class BiometricOnly(private val biometricInterface: BiometricHandler,
                    private val authentication: KeyAware,
                    private val isApi30OrAbove: Boolean): Authenticator {

    /**
     * generate the public and private keypair
     */
    @SuppressLint("NewApi")
    override fun generateKeys(): KeyPair {
        val builder = authentication.keyBuilder()
        if (isApi30OrAbove) {
            builder.setUserAuthenticationParameters(authentication.timeout, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(authentication.timeout)
        }
        builder.setUserAuthenticationRequired(true)
        return authentication.createKeyPair(builder)
    }

    /**
     * check biometric is supported
     */
    override fun isSupported(): Boolean {
        return biometricInterface.isSupported(BIOMETRIC_STRONG, BIOMETRIC_WEAK)
    }

    /**
     * Display biometric prompt for authentication type
     * @param timeout Timeout for biometric prompt
     * @param statusResult Listener for receiving Biometric changes
     */
    override fun authenticate(timeout: Int,  statusResult: (DeviceBindingStatus) -> Unit) {
        biometricInterface.authenticate(timeout, statusResult)
    }


}

/**
 * Settings for all the biometric authentication and device credential is configured
 */
internal class BiometricAndDeviceCredential(private val biometricInterface: BiometricHandler,
                                   private val keyAware: KeyAware,
                                   private val isApi30OrAbove: Boolean): Authenticator {

    /**
     * generate the public and private keypair
     */
    @SuppressLint("NewApi")
    override fun generateKeys(): KeyPair {
        val builder = keyAware.keyBuilder()
        if (isApi30OrAbove) {
            builder.setUserAuthenticationParameters(keyAware.timeout, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(keyAware.timeout)
        }
        builder.setUserAuthenticationRequired(true)
        return keyAware.createKeyPair(builder)
    }

    /**
     * check biometric is supported
     */
    override fun isSupported(): Boolean {
        return biometricInterface.isSupported(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
    }

    /**
     * Display biometric prompt for authentication type
     * @param timeout Timeout for biometric prompt
     * @param statusResult Listener for receiving Biometric changes
     */
    override fun authenticate(timeout: Int,  statusResult: (DeviceBindingStatus) -> Unit) {
        biometricInterface.authenticate(timeout, statusResult)
    }

}

/**
 * Settings for all the none authentication is configured
 */
internal class None(private val keyAware: KeyAware): Authenticator {
    /**
     * generate the public and private keypair
     */
    override fun generateKeys(): KeyPair {
        val builder = keyAware.keyBuilder()
        return keyAware.createKeyPair(builder)
    }

    /**
     * Default is true for None type
     */
    override fun isSupported(): Boolean {
        return true
    }

    /**
     * return success block for None type
     */
    override fun authenticate(
        timeout: Int,
        result: (DeviceBindingStatus) -> Unit) {
        result(Success)
    }

}

/**
 * Internal AuthenticatorFactory to create the authentication type.
 */
internal class AuthenticatorFactory {
    companion object {
        fun getType(
            userId: String,
            authentication: DeviceBindingAuthenticationType,
            title: String,
            subtitle: String,
            description: String,
            keyAware: KeyAware = KeyAware(userId),
            isApi30OrAbove: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        ): Authenticator {
            return when (authentication) {
                DeviceBindingAuthenticationType.BIOMETRIC_ONLY -> BiometricOnly(
                    biometricInterface = BiometricBindingHandler(
                        title,
                        subtitle,
                        description,
                        deviceBindAuthenticationType = authentication
                    ),
                    keyAware,
                    isApi30OrAbove
                )
                DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK -> BiometricAndDeviceCredential(
                    BiometricBindingHandler(
                        title,
                        subtitle,
                        description,
                        deviceBindAuthenticationType = authentication
                    ),
                    keyAware,
                    isApi30OrAbove
                )
                else -> None(keyAware)
            }
        }
    }
}

