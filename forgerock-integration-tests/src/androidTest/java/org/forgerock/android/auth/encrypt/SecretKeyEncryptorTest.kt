/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.encrypt

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.security.KeyStore.PrivateKeyEntry
import java.security.KeyStore.SecretKeyEntry

@RunWith(AndroidJUnit4::class)
@SmallTest
class SecretKeyEncryptorTest {

    private val alias = "keystore-key"
    private val appContext: Context by lazy { ApplicationProvider.getApplicationContext<Application>() }

    private val keyStore: KeyStore
        get() {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            return keyStore
        }

    @After
    fun tearDown() {
        keyStore.deleteEntry(alias)
    }

    @Test
    fun testEncryptWithAsymmetricKey() = runTest {
        val encryptor = SecretKeyEncryptor {
            context = appContext
            keyAlias = alias
            enforceAsymmetricKey = true
        }

        val encrypted = encryptor.encrypt("test".toByteArray())
        val decrypted = encryptor.decrypt(encrypted)
        assertEquals("test", decrypted.toString(Charsets.UTF_8))

        //Make sure the key is stored in the keystore as a private key
        assertTrue(keyStore.getEntry(alias, null) is PrivateKeyEntry)
    }

    @Test
    fun testEncryptWithSymmetricKey() = runTest {
        val encryptor = SecretKeyEncryptor {
            context = appContext
            keyAlias = alias
        }

        val encrypted = encryptor.encrypt("test".toByteArray())
        val decrypted = encryptor.decrypt(encrypted)
        assertEquals("test", decrypted.toString(Charsets.UTF_8))

        //Make sure the secret key is not generated
        assertTrue(keyStore.getEntry(alias, null) is SecretKeyEntry)
    }

    @Test
    fun testEncryptWithSymmetricKeyThenAsymmetric() = runTest {
        val encryptor = SecretKeyEncryptor {
            context = appContext
            keyAlias = alias
        }

        val encrypted = encryptor.encrypt("test".toByteArray())
        val decrypted = encryptor.decrypt(encrypted)
        assertEquals("test", decrypted.toString(Charsets.UTF_8))

        //using the same alias, now switch to asymmetric key
        val encryptor2 = SecretKeyEncryptor {
            context = appContext
            keyAlias = alias
            enforceAsymmetricKey = true
        }

        //Since it was encrypted with a symmetric key, keep using the symmetric key to decrypt
        val decrypted2 = encryptor2.decrypt(encrypted)
        assertEquals("test", decrypted2.toString(Charsets.UTF_8))

        encryptor2.encrypt("test".toByteArray())
        //The key is now stored as a private key after encrypt
        assertTrue(keyStore.getEntry(alias, null) is PrivateKeyEntry)
    }

}