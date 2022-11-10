package org.forgerock.android.auth

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.UnrecoverableKeyException

@RunWith(AndroidJUnit4::class)
class AppPinAuthenticatorTest {

    private val context = mock<Context>()
    private lateinit var appPinAuthenticator: AppPinAuthenticator
    private val cryptoKey = CryptoKey("dummy")
    private val repository = object : KeyStoreRepository {
        var byteArrayOutputStream = ByteArrayOutputStream(1024)

        override fun getInputStream(context: Context, identifier: String): InputStream {
            return byteArrayOutputStream.toByteArray().inputStream();
        }

        override fun getOutputStream(context: Context, identifier: String): OutputStream {
            return byteArrayOutputStream
        }

        override fun getKeystoreType(): String {
            return "BKS"
        }
    }

    @Before
    fun setUp() {
        appPinAuthenticator = AppPinAuthenticator(cryptoKey, repository)
    }

    @After
    fun tearDown() {
        repository.byteArrayOutputStream = ByteArrayOutputStream(1024)
    }

    @Test
    fun testSuccessAuthenticate() {
        appPinAuthenticator.generateKeys(context, "1234".toCharArray())
        val privateKey = appPinAuthenticator.authenticate(context, "1234".toCharArray())
        assertThat(privateKey).isNotNull
    }

    @Test(expected = UnrecoverableKeyException::class)
    fun testInvalidPin() {
        appPinAuthenticator.generateKeys(context, "1234".toCharArray())
        appPinAuthenticator.authenticate(context, "invalid".toCharArray())
    }

    @Test(expected = IOException::class)
    fun testNoKeys() {
        appPinAuthenticator.authenticate(context, "invalid".toCharArray())
    }

    @Test
    fun testAlias() {
        appPinAuthenticator.generateKeys(context, "1234".toCharArray())
        assertThat(appPinAuthenticator.getKeyAlias()).isEqualTo(cryptoKey.keyAlias)
    }

    @Test
    fun testKeyStore() {
        assertThat(repository.byteArrayOutputStream.toByteArray().size).isEqualTo(0)
        appPinAuthenticator.generateKeys(context, "1234".toCharArray())
        assertThat(repository.byteArrayOutputStream.toByteArray().size).isNotCloseTo(0,
            Offset.offset(1000))
    }
}