/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.security.*
import java.security.interfaces.RSAPublicKey


/**
 * Helper class to generate and sign the keys
 */
internal class KeyAware(private var userId: String) {

    constructor() : this("")

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

    /**
     * Builder to create a keypair
     */
    fun keyBuilder(): KeyGenParameterSpec.Builder {
        return KeyGenParameterSpec.Builder(
            key,
            purpose
        ).setDigests(KeyProperties.DIGEST_SHA512)
            .setKeySize(keySize)
            .setSignaturePaddings(signaturePadding)
            .setBlockModes(encryptionBlockMode)
            .setEncryptionPaddings(encryptionPadding)
    }

    /**
     * Creates Keypair for the given builder
     * @param builder keygen parameter as input to get the keypair
     */
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


    /**
     * Get the private key from the Keypair for the given builder
     * @param keyAlias key hash of the user
     */
    fun getSecureKey(keyAlias: String = key): PrivateKey? {
        val keyStore: KeyStore = getKeyStore()
        return keyStore.getKey(keyAlias, null) as? PrivateKey
    }

    /**
     * get hash for the given user
     * @param keyName username as a key
     */
    private fun getKeyAlias(keyName: String = userId): String {
        return try {
            val digest: MessageDigest = MessageDigest.getInstance(hashingAlgorithm)
            val hash: ByteArray? = digest.digest(keyName.toByteArray())
            Base64.encodeToString(hash, Base64.DEFAULT)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
        catch (e: Exception) {
            throw RuntimeException(e)
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

}