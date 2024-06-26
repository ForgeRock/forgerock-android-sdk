/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.nfc.Tag
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Provide data encryption and decryption for Android M device.
 */
internal open class AndroidMEncryptor(keyAlias: String) : AbstractSymmetricEncryptor(keyAlias) {
    @JvmField
    val specBuilder: KeyGenParameterSpec.Builder = KeyGenParameterSpec.Builder(
        keyAlias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .setUserAuthenticationRequired(false)
        .setKeySize(KEY_SIZE)

    @Throws(GeneralSecurityException::class, IOException::class)
    override fun getSecretKey(): SecretKey {
        keyReferenceCache.get()?.let {
            Logger.debug(tag, "Secret Key retrieved from cache")
            return it
        }
        if (keyStore.containsAlias(keyAlias)) {
            return (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey.also {
                Logger.debug(tag, "Secret Key retrieved from KeyStore and stored in cache")
                keyReferenceCache.set(it)
            }
        } else {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
            )
            keyGenerator.init(specBuilder.build())
            return keyGenerator.generateKey().also {
                Logger.debug(tag, "Secret Key generated and stored in cache")
                keyReferenceCache.set(it)
            }
        }
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    public override fun init(cipher: Cipher): ByteArray {
        //Generate a random IV See KeyGenParameterSpec.Builder.setRandomizedEncryptionRequired
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.iv
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    override fun reset() {
        keyStore.deleteEntry(keyAlias)
        keyReferenceCache.set(null)
        Logger.debug(tag, "Secret Key removed from KeyStore and cache")
    }

    companion object {
        val tag: String = AndroidMEncryptor::class.java.simpleName
        //Hold the current key.
         val keyReferenceCache = AtomicReference<SecretKey>()

        /**
         * Retrieve and load the Android KeyStore
         *
         * @return The AndroidKeyStore
         */
        @get:Throws(GeneralSecurityException::class, IOException::class)
        private val keyStore: KeyStore
            get() {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
                keyStore.load(null)
                return keyStore
            }
    }
}