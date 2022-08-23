/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.annotation.SuppressLint
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import java.io.IOException
import java.security.*
import java.security.interfaces.RSAPublicKey

interface AuthenticationInterface {
    fun generateKeys(): KeyPair
    fun authenticate(timeout: Int,  statusResult: (BiometricStatus) -> Unit)
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
    fun isSupported(): Boolean
}

data class KeyPair(
     val publicKey: RSAPublicKey,
     val privateKey: PrivateKey,
     var keyAlias: String
)

class DeviceBindAuthentication(private var userId: String) {
    companion object {
        fun getType(userId: String,
                    authentication: DeviceBindingAuthenticationType,
                    title: String,
                    subtitle: String,
                    description: String,
                    deviceBindAuthentication: DeviceBindAuthentication = DeviceBindAuthentication(userId)): AuthenticationInterface {
            return when (authentication) {
                DeviceBindingAuthenticationType.BIOMETRIC_ONLY -> BiometricOnly(biometricInterface = BiometricUtil(title, subtitle, description, deviceBindAuthenticationType = authentication), deviceBindAuthentication)
                DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK -> BiometricAndDeviceCredential(BiometricUtil(title, subtitle, description, deviceBindAuthenticationType = authentication), deviceBindAuthentication)
                else -> None(deviceBindAuthentication)
            }
        }
    }

    private val hashingAlgorithm = "SHA-256"
    private val keySize = 2048
    val timeout = 60
    private val androidKeyStore = "AndroidKeyStore"
    private val encryptionBlockMode = KeyProperties.BLOCK_MODE_ECB
    private val encryptionPadding = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
    private val encryptionAlgorithm = KeyProperties.KEY_ALGORITHM_RSA
    private val signaturePadding = KeyProperties.SIGNATURE_PADDING_RSA_PKCS1
    private val purpose = KeyProperties.PURPOSE_SIGN or
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    private val key = getKeyAlias(userId)
    val isApi30AndAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

     fun keyBuilder(): KeyGenParameterSpec.Builder {
        val key = getKeyAlias()
         return KeyGenParameterSpec.Builder(
            key,
            purpose
        ).setDigests(KeyProperties.DIGEST_SHA512)
            .setKeySize(keySize)
            .setSignaturePaddings(signaturePadding)
            .setBlockModes(encryptionBlockMode)
            .setEncryptionPaddings(encryptionPadding)
    }

    fun createKeyPair(builder: KeyGenParameterSpec.Builder): KeyPair {
        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(
            encryptionAlgorithm, androidKeyStore
        )
        keyPairGenerator.initialize(builder.build())
        keyPairGenerator.generateKeyPair()

        val keyStore = getKeyStore()
        val publicKey = keyStore.getCertificate(key).publicKey as RSAPublicKey
        val privateKey = keyStore.getKey(key, null) as PrivateKey

        return KeyPair(publicKey, privateKey, key)
    }

    internal fun getKeyAlias(keyName: String = userId): String {
        return getHash(keyName)
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

    private fun getHash(value: String): String {
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

class BiometricOnly(private val biometricInterface: BiometricInterface,
                    private val authentication: DeviceBindAuthentication): AuthenticationInterface {

    @SuppressLint("NewApi")
    override fun generateKeys(): KeyPair {
            val builder = authentication.keyBuilder()
            if (authentication.isApi30AndAbove) {
                builder.setUserAuthenticationParameters(authentication.timeout, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                builder.setUserAuthenticationValidityDurationSeconds(authentication.timeout)
            }
            builder.setUserAuthenticationRequired(true)
           return authentication.createKeyPair(builder)
    }

    override fun isSupported(): Boolean {
        return biometricInterface.isSupportedForBiometricOnly()
    }

    override fun authenticate(timeout: Int,  statusResult: (BiometricStatus) -> Unit) {
        val listener = biometricInterface.getBiometricListener(timeout, statusResult)
        biometricInterface.setListener(listener)
        biometricInterface.authenticate()
    }

}

class BiometricAndDeviceCredential(private val biometricInterface: BiometricInterface,
                                   private val authentication: DeviceBindAuthentication): AuthenticationInterface {

    @SuppressLint("NewApi")
    override fun generateKeys(): KeyPair {
        val builder = authentication.keyBuilder()
        if (authentication.isApi30AndAbove) {
            builder.setUserAuthenticationParameters(authentication.timeout, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(authentication.timeout)
        }
        builder.setUserAuthenticationRequired(true)
        return authentication.createKeyPair(builder)
    }

    override fun isSupported(): Boolean {
        return biometricInterface.isSupportedForBiometricOnly()
    }

    override fun authenticate(timeout: Int,  statusResult: (BiometricStatus) -> Unit) {
        val listener = biometricInterface.getBiometricListener(timeout, statusResult)
        biometricInterface.setListener(listener)
        biometricInterface.authenticate()
    }
}

class None(private val authentication: DeviceBindAuthentication): AuthenticationInterface {
    override fun generateKeys(): KeyPair {
        val builder = authentication.keyBuilder()
        return authentication.createKeyPair(builder)
    }

    override fun isSupported(): Boolean {
        return true
    }

    override fun authenticate(
        timeout: Int,
        result: (BiometricStatus) -> Unit) {
        result(BiometricStatus(true, null, null))
    }
}