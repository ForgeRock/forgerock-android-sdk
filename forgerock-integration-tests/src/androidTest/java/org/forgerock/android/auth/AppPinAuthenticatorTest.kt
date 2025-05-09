/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.security.UnrecoverableKeyException

@RunWith(AndroidJUnit4::class)
class AppPinAuthenticatorTest {

    private lateinit var appPinAuthenticator: AppPinAuthenticator
    private val cryptoKey = CryptoKey("dummy")
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        appPinAuthenticator = AppPinAuthenticator(cryptoKey)
    }

    @After
    fun tearDown() {
        EncryptedFileKeyStore(cryptoKey.keyAlias).delete(context)
    }

    @Test
    fun testSuccessAuthenticate() {
        appPinAuthenticator.generateKeys(context, "1234".toCharArray())
        val privateKey = appPinAuthenticator.getPrivateKey(context, "1234".toCharArray())
        Assertions.assertThat(privateKey).isNotNull
    }

    @Test
    fun testInvalidPin() {
        appPinAuthenticator.generateKeys(context, "1234".toCharArray())
        try {
            appPinAuthenticator.getPrivateKey(context, "invalid".toCharArray())
            fail()
        }
        catch (e: UnrecoverableKeyException) {
            assertTrue(true)
        }
        catch (e: IOException) {
            assertTrue(true)
        }
    }

    @Test(expected = IOException::class)
    fun testNoKeys() {
        appPinAuthenticator.getPrivateKey(context, "invalid".toCharArray())
    }

    @Test
    fun testAlias() {
        appPinAuthenticator.generateKeys(context, "1234".toCharArray())
        Assertions.assertThat(appPinAuthenticator.getKeyAlias()).isEqualTo(cryptoKey.keyAlias)
    }

    @Test
    fun testExists() {
        Assertions.assertThat(appPinAuthenticator.exists(context)).isFalse()
        appPinAuthenticator.generateKeys(context, "1234".toCharArray())
        Assertions.assertThat(appPinAuthenticator.exists(context)).isTrue()
    }
}