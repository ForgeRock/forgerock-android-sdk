/*
 * Copyright (c) 2024. PingIdentity. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.encrypt

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.forgerock.android.auth.Logger
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

private const val TAG = "SecretKeyEncryptor"
private const val AES_GCM_NO_PADDING = "AES/GCM/NOPADDING"
private const val HMAC_SHA256 = "HmacSHA256"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val RSA_ECB_OAEP_PADDING = "RSA/ECB/OAEPPadding"
private const val IV_LENGTH = 12

/**
 * Configuration class for `SecretKeyEncryptor`.
 * It contains various properties that can be set to configure the encryption process.
 *
 * @property context The Android context used for accessing system services.
 * @property keyAlias The alias for the key in the Android keystore.
 * `enforceAsymmetricKey` is set to `true` or failed to generate Android keystore secret key.
 * @property enforceAsymmetricKey Flag to enforce the use of an asymmetric key.
 * @property throwWhenEncryptError Flag to throw an exception when an encryption/decryption error occurs.
 * @property symmetricKeySize The size of the symmetric key.
 * @property invalidatedByBiometricEnrollment Flag to invalidate the key by biometric enrollment.
 */
class SecretKeyEncryptorConfig {
    lateinit var context: Context
    lateinit var keyAlias: String
    var enforceAsymmetricKey = false
    var throwWhenEncryptError = true
    var symmetricKeySize = 256
    var invalidatedByBiometricEnrollment = true

}

/**
 * An encryptor that uses Android's SecretKey to encrypt and decrypt data.
 * It uses AES/GCM/NoPadding as the cipher and HmacSHA256 for the MAC.
 */
class SecretKeyEncryptor(block: SecretKeyEncryptorConfig.() -> Unit = {}) : SuspendEncryptor {
    val config = SecretKeyEncryptorConfig().apply(block)
    private val lock = Mutex()
    private val mac: Mac
    private val macLength: Int

    private val secretKeyGenerator by lazy {
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES).apply {
            init((config.symmetricKeySize))
        }
    }

    init {
        mac = Mac.getInstance(HMAC_SHA256)
        val sk: SecretKey =
            SecretKeySpec(config.keyAlias.toByteArray(StandardCharsets.UTF_8), HMAC_SHA256)
        mac.init(sk)
        macLength = mac.macLength
    }

    /**
     * Encrypts the given data.
     * It uses a lock to ensure thread safety.
     * @param data The data to encrypt.
     * @return The encrypted data.
     */
    override suspend fun encrypt(data: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        lock.withLock {
            withRetry(byteArrayOf(), {
                Logger.error(TAG, it, "Failed to encrypt data, retrying...")
                keyStore.deleteEntry(config.keyAlias)
            }) {
                Logger.debug(TAG, "Encrypting data...")
                val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
                val symmetricKey = secretKey()
                cipher.init(Cipher.ENCRYPT_MODE, symmetricKey.secretKey)
                val iv = cipher.iv
                val encryptedData = cipher.doFinal(data)
                val mac = mac.doFinal(symmetricKey.encoded + encryptedData)
                mac + iv + symmetricKey.encoded + encryptedData
            }
        }
    }

    /**
     * Decrypts the given data.
     * It uses a lock to ensure thread safety.
     * @param data The data to decrypt.
     * @return The decrypted data.
     */
    override suspend fun decrypt(data: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        lock.withLock {
            try {
                Logger.debug(TAG, "Decrypting data...")
                val cipher =
                    Cipher.getInstance(AES_GCM_NO_PADDING)
                val macFromMessage = data.copyOfRange(0, macLength)
                val iv = data.copyOfRange(macLength, macLength + IV_LENGTH)
                val encryptedData = data.copyOfRange(macLength + IV_LENGTH, data.size)
                val mac = mac.doFinal(encryptedData)

                if (!mac.contentEquals(macFromMessage)) {
                    throw RuntimeException("MAC signature could not be verified")
                }
                val ivParams: AlgorithmParameterSpec = GCMParameterSpec(128, iv)

                val symmetricKey = secretKey(encryptedData)
                cipher.init(Cipher.DECRYPT_MODE, symmetricKey.secretKey, ivParams)
                cipher.doFinal(
                    encryptedData.copyOfRange(
                        symmetricKey.encoded.size,
                        encryptedData.size
                    )
                )
            } catch (e: Throwable) {
                Logger.error(TAG, e, "Failed to decrypt data")
                if (config.throwWhenEncryptError) throw e
                byteArrayOf()
            }
        }
    }

    /**
     * Retrieves the secret key for encryption
     *
     * @return The symmetric key.
     */
    private suspend fun secretKey(): SymmetricKey = withContext(Dispatchers.IO) {
        //Check if the key exists in the keystore
        val key = keyStore.getEntry(config.keyAlias, null)
        key?.let {
            when (it) {
                is KeyStore.SecretKeyEntry -> {
                    if (config.enforceAsymmetricKey) {
                        //It was using symmetric key, now switch to asymmetric key
                        generateEmbeddedSecretKey()
                    } else {
                        //return the generated SecretKey
                        SymmetricKey(it.secretKey)
                    }
                }

                //If the key is a PrivateKeyEntry, retrieve the SecretKey
                is KeyStore.PrivateKeyEntry -> {
                    generateEmbeddedSecretKey(it.certificate.publicKey)
                }

                else -> throw IllegalStateException("KeyStore entry is not a SecretKeyEntry or PrivateKeyEntry")
            }
        } ?: run {
            try {
                SymmetricKey(generateAndroidKeyStoreSecretKey())
            } catch (e: Throwable) {
                Logger.warn(TAG, e, "falling back to asymmetric key")
                //If failed to generate Android keystore secret key, generate file-based secret key
                generateEmbeddedSecretKey()
            }
        }
    }

    private suspend fun secretKey(encryptedData: ByteArray): SymmetricKey =
        withContext(Dispatchers.IO) {
            //Check if the key exists in the keystore
            val key = keyStore.getEntry(config.keyAlias, null)
            key?.let {
                when (it) {
                    //If the key is a SecretKeyEntry, return the SecretKey
                    is KeyStore.SecretKeyEntry -> {
                        if (config.enforceAsymmetricKey) {
                            Logger.warn(TAG,
                                "SecretKey was generated in AndroidKeyStore, Enforcing asymmetric key is ignored.")
                        }
                        //return the generated SecretKey
                        SymmetricKey(it.secretKey)
                    }

                    //If the key is a PrivateKeyEntry, retrieve the SecretKey
                    is KeyStore.PrivateKeyEntry -> {
                        getEmbeddedSecretKey(
                            it.privateKey,
                            encryptedData
                        )
                    }

                    else -> throw IllegalStateException("KeyStore entry is not a SecretKeyEntry or PrivateKeyEntry")
                }
            } ?: run {
                throw IllegalStateException("SecretKey not found")
            }
        }

    /**
     * Generates an embedded secret key.
     * If a private key is provided, it uses it to encrypt the secret key.
     * Otherwise, it generates a new asymmetric key pair in the Android keystore.
     *
     * @param privateKey The private key used to encrypt the secret key. If null, a new key pair is generated.
     * @return The generated symmetric key.
     */
    private fun generateEmbeddedSecretKey(publicKey: PublicKey? = null): SymmetricKey {

        val key = publicKey ?: run {
            generateAndroidKeyStoreAsymmetricKey()
            keyStore.getCertificate(config.keyAlias).publicKey
        }
        val cipher = Cipher.getInstance(RSA_ECB_OAEP_PADDING).apply {
            init(
                Cipher.ENCRYPT_MODE,
                key,
                OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA1,
                    PSource.PSpecified.DEFAULT
                )
            )
        }
        val secretKey = secretKeyGenerator.generateKey()
        val encryptedSecretKey = cipher.doFinal(secretKey.encoded)

        return SymmetricKey(secretKey, encryptedSecretKey.size.toByteArray() + encryptedSecretKey)
    }


    /**
     * Generates an asymmetric key pair in the Android keystore.
     * This method configures the key generation parameters, including key size, block modes,
     * encryption paddings, and user authentication requirements. It also handles the case
     * where StrongBox is unavailable and falls back to generating the key without StrongBox support.
     */
    private fun generateAndroidKeyStoreAsymmetricKey() {
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            config.keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setKeySize(2048)
            .setDigests(
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA512
            )
            .setUserAuthenticationRequired(false)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //Add in Level 24
            keyGenParameterSpec.setInvalidatedByBiometricEnrollment(config.invalidatedByBiometricEnrollment)
        }

        //Allow access the data during screen lock
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            keyGenParameterSpec.setUnlockedDeviceRequired(false)
            if (config.context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
                keyGenParameterSpec.setIsStrongBoxBacked(true)
            }
        }

        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE
        )

        keyPairGenerator.initialize(keyGenParameterSpec.build())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                keyPairGenerator.generateKeyPair()
            } catch (e: Exception) {
                //Retry again without strong box enabled, some platform may not response with StrongBoxUnavailableException
                Logger.warn(TAG, e, "Strong Box unavailable, recover without strong box")
                keyGenParameterSpec.setIsStrongBoxBacked(false)
                keyPairGenerator.initialize(keyGenParameterSpec.build())
                keyPairGenerator.generateKeyPair()
            }
        } else {
            keyPairGenerator.generateKeyPair()
        }
    }

    /**
     * Retrieves the embedded secret key from the encrypted data using the provided private key.
     *
     * @param privateKey The private key used to decrypt the embedded secret key.
     * @param encryptedData The data containing the encrypted secret key.
     * @return The decrypted symmetric key.
     */
    private fun getEmbeddedSecretKey(
        privateKey: PrivateKey,
        encryptedData: ByteArray
    ): SymmetricKey {
        // Extract the length of the encrypted secret key from the encrypted data
        val encryptedSecretKeyLength = encryptedData.toInt()
        val encryptedSecretKey = encryptedData.copyOfRange(4, 4 + encryptedSecretKeyLength)

        // Initialize the cipher for decryption using RSA/ECB/OAEPPadding
        val cipher = Cipher.getInstance(RSA_ECB_OAEP_PADDING).apply {
            init(
                Cipher.DECRYPT_MODE,
                privateKey,
                OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA1,
                    PSource.PSpecified.DEFAULT
                )
            )
        }

        // Decrypt the secret key and return it as a SymmetricKey
        return SymmetricKey(
            SecretKeySpec(cipher.doFinal(encryptedSecretKey), KeyProperties.KEY_ALGORITHM_AES),
            encryptedData.copyOfRange(0, 4 + encryptedSecretKeyLength)
        )
    }

    /**
     * Generates a secret key in the Android keystore.
     * This method configures the key generation parameters, including key size, block modes,
     * encryption paddings, and user authentication requirements. It also handles the case
     * where StrongBox is unavailable and falls back to generating the key without StrongBox support.
     *
     * @return The generated secret key.
     */
    private fun generateAndroidKeyStoreSecretKey(): SecretKey {
        if (config.enforceAsymmetricKey) {
            throw IllegalStateException("Enforcing asymmetric key, do not generating symmetric key.")
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )

        val specBuilder: KeyGenParameterSpec.Builder = KeyGenParameterSpec.Builder(
            config.keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
            .setKeySize(config.symmetricKeySize)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //Add in Level 24
            specBuilder.setInvalidatedByBiometricEnrollment(config.invalidatedByBiometricEnrollment)
        }

        //Allow access the data during screen lock
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            specBuilder.setUnlockedDeviceRequired(false)
            if (config.context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
                specBuilder.setIsStrongBoxBacked(true)
            }
        }

        keyGenerator.init(specBuilder.build())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return keyGenerator.generateKey()
            } catch (e: Exception) {
                //Retry again without strong box enabled, some platform may not response with StrongBoxUnavailableException
                Logger.warn(TAG, e, "Strong Box unavailable, recover without strong box")
                specBuilder.setIsStrongBoxBacked(false)
                keyGenerator.init(specBuilder.build())
                return keyGenerator.generateKey()
            }
        } else {
            return keyGenerator.generateKey()
        }
    }

    companion object {
        /**
         * Returns the Android keystore.
         * @return The Android keystore.
         */
        internal val keyStore: KeyStore
            get() {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                return keyStore
            }
    }

    /**
     * Executes the given block and retries if an exception is thrown.
     * @param reset A function to execute if an exception is thrown.
     * @param block The block to execute.
     * @return The result of the block.
     */
    private inline fun <T> withRetry(
        default: T,
        reset: (Throwable) -> Unit = {},
        block: () -> T
    ): T {
        return try {
            block()
        } catch (e: Throwable) {
            reset(e)
            try {
                block()
            } catch (e: Throwable) {
                if (config.throwWhenEncryptError) throw e
                return default
            }
        }
    }
}

/**
 * Converts an Int to a ByteArray.
 *
 * @return A ByteArray representing the integer value.
 */
private fun Int.toByteArray(): ByteArray {
    return ByteBuffer.allocate(4).putInt(this).array()
}

/**
 * Converts a ByteArray to an Int.
 *
 * @return The integer value represented by the first 4 bytes of the ByteArray.
 */
private fun ByteArray.toInt(): Int {
    return ByteBuffer.wrap(this, 0, 4).int
}