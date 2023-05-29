package org.forgerock.android.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
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

    @Test(expected = UnrecoverableKeyException::class)
    fun testInvalidPin() {
        appPinAuthenticator.generateKeys(context, "1234".toCharArray())
        appPinAuthenticator.getPrivateKey(context, "invalid".toCharArray())
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