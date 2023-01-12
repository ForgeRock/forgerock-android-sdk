/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.security.*
import java.security.interfaces.RSAPublicKey
import java.security.spec.AlgorithmParameterSpec


/**
 * Helper class to generate and sign the keys
 */
class CryptoKey(private var keyId: String) {

    //For hashing the keyId
    private val hashingAlgorithm = "SHA-256"
    val keySize = 2048
    val timeout = 60
    private val androidKeyStore = "AndroidKeyStore"
    private val encryptionBlockMode = KeyProperties.BLOCK_MODE_ECB
    private val encryptionPadding = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
    private val encryptionAlgorithm = KeyProperties.KEY_ALGORITHM_RSA
    private val signaturePadding = KeyProperties.SIGNATURE_PADDING_RSA_PKCS1
    private val purpose =
        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    val keyAlias = getKeyAlias(keyId)

    /**
     * Builder to create a keypair
     */
    fun keyBuilder(): KeyGenParameterSpec.Builder {
        return KeyGenParameterSpec.Builder(keyAlias, purpose)
            .setDigests(KeyProperties.DIGEST_SHA512).setKeySize(keySize)
            .setSignaturePaddings(signaturePadding).setBlockModes(encryptionBlockMode)
            .setEncryptionPaddings(encryptionPadding)
    }

    /**
     * Creates Keypair for the given builder
     * @param spec keygen parameter as input to get the keypair
     */
    fun createKeyPair(spec: AlgorithmParameterSpec, useAndroidKeyStore: Boolean = true): KeyPair {

        val keyPairGenerator = if (useAndroidKeyStore) {
            KeyPairGenerator.getInstance(encryptionAlgorithm, androidKeyStore)
        } else {
            KeyPairGenerator.getInstance(encryptionAlgorithm)
        }

        keyPairGenerator.initialize(spec)
        val keyPair = keyPairGenerator.generateKeyPair();

        return KeyPair(keyPair.public as RSAPublicKey, keyPair.private)
    }

    /**
     * Get the private key from the Keypair for the given builder
     */
    fun getPrivateKey(): PrivateKey? {
        val keyStore: KeyStore = getKeyStore()
        return keyStore.getKey(keyAlias, null) as? PrivateKey
    }

    /**
     * Delete keys from Android KeyStore
     */
    fun deleteKeys() {
        val keyStore: KeyStore = getKeyStore()
        keyStore.deleteEntry(keyAlias)
    }

    /**
     * get hash for the given user
     * @param keyName username as a key
     */
    private fun getKeyAlias(keyName: String = keyId): String {
        return try {
            val digest: MessageDigest = MessageDigest.getInstance(hashingAlgorithm)
            val hash: ByteArray? = digest.digest(keyName.toByteArray())
            Base64.encodeToString(hash, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * Creates centralised SecretKey using the KeyStore
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getKeyStore(type: String = androidKeyStore): KeyStore {
        val keyStore = KeyStore.getInstance(type)
        keyStore.load(null)
        return keyStore
    }
}