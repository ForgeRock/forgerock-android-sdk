/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.forgerock.android.auth.AppPinAuthenticator
import org.forgerock.android.auth.CryptoKey
import org.forgerock.android.auth.InitProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ApplicationPinDeviceAuthenticatorTest {

    private val activity = mock<FragmentActivity>()
    private val cryptoKey = CryptoKey("bob")
    var context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        whenever(activity.applicationContext).thenReturn(ApplicationProvider.getApplicationContext())
        InitProvider.setCurrentActivity(activity)
        val keystoreFile = File(activity.filesDir, cryptoKey.keyAlias)
        if (keystoreFile.exists()) {
            keystoreFile.delete()
        }
    }

    @After
    fun tearDown() {
        InitProvider.setCurrentActivity(null);
        val keystoreFile = File(activity.filesDir, cryptoKey.keyAlias)
        if (keystoreFile.exists()) {
            keystoreFile.delete()
        }
    }

    private fun generateKeys() {
        val authenticator = getApplicationPinDeviceAuthenticator()
        authenticator.generateKeys(context) {}
    }

    @Test
    fun testIsSupported() {
        val authenticator = getApplicationPinDeviceAuthenticator()
        assertThat(authenticator.isSupported(context)).isTrue()
    }

    @Test
    fun testGenerateKeys() {
        val authenticator = getApplicationPinDeviceAuthenticator()
        authenticator.generateKeys(context) {
            assertThat(it).isNotNull
            assertThat(it.keyAlias).isEqualTo(cryptoKey.keyAlias + "_PIN")
            val keystoreFile = File(activity.applicationContext.filesDir, cryptoKey.keyAlias)
            assertThat(keystoreFile.exists()).isTrue()
        }
        //Test pin is cached for 1 sec
        assertThat(authenticator.pinRef.get()).isNotNull()
        authenticator.worker.awaitTermination(3, TimeUnit.SECONDS)
        assertThat(authenticator.pinRef.get()).isNull()
    }

    @Test
    fun testSuccessAuthenticated() {
        generateKeys()
        val authenticator = getApplicationPinDeviceAuthenticator()
        authenticator.generateKeys(context) {
            val keyPair = it
            authenticator.authenticate(context, 60) {
                assertThat(it).isEqualTo(Success(keyPair.privateKey))
                assertThat(authenticator.pinRef.get()).isNull()
            }
        }
    }


    // Its hard to test this scenario without mocking, the keystore has to return null value to test this case
    @Test
    fun testUnRegisterWhenPrivateKeyIsNull() {
        val authenticator: ApplicationPinDeviceAuthenticator = getApplicationPinDeviceAuthenticator()
        val mockAppPinAuthenticator = mock<AppPinAuthenticator>()
        whenever(mockAppPinAuthenticator.getPrivateKey(any(), any())).thenReturn(null)
        authenticator.appPinAuthenticator = mockAppPinAuthenticator
        authenticator.pinRef.set("1234".toCharArray())
        authenticator.authenticate(context, 100) {
            assertThat(it).isEqualTo(UnRegister())
        }
    }

    @Test
    fun testUnRegister() {
        val authenticator = getApplicationPinDeviceAuthenticator()
        authenticator.authenticate(context, 60) {
            assertThat(it).isEqualTo(UnRegister())
        }
    }

    //Provide wrong pin
    @Test
    fun testUnAuthorize() {
        generateKeys()
        val authenticator = object : NoEncryptionApplicationPinDeviceAuthenticator() {
            override fun requestForCredentials(fragmentActivity: FragmentActivity,
                                               onCredentialsReceived: (CharArray) -> Unit) {
                onCredentialsReceived("invalidPin".toCharArray())
            }
        }
        authenticator.setKey(cryptoKey)
        authenticator.authenticate(context, 60) {
            assertThat(it).isEqualTo(UnAuthorize())
        }
    }

    @Test
    fun testTimeout() {
        generateKeys()
        val authenticator = getApplicationPinDeviceAuthenticator()
        authenticator.authenticate(context, -1) {
            assertThat(it).isEqualTo(Timeout())
        }
    }

    @Test
    fun testAbort() {
        generateKeys()
        val authenticator = object : NoEncryptionApplicationPinDeviceAuthenticator() {
            override fun requestForCredentials(fragmentActivity: FragmentActivity,
                                               onCredentialsReceived: (CharArray) -> Unit) {
                onCredentialsReceived(CharArray(0))
            }
        }
        authenticator.authenticate(context, 60) {
            assertThat(it).isEqualTo(Abort())
        }
    }

    private fun getApplicationPinDeviceAuthenticator(): ApplicationPinDeviceAuthenticator {
        val authenticator = NoEncryptionApplicationPinDeviceAuthenticator()
        authenticator.setKey(cryptoKey)
        return authenticator
    }

    open class NoEncryptionApplicationPinDeviceAuthenticator : ApplicationPinDeviceAuthenticator() {
        override fun requestForCredentials(fragmentActivity: FragmentActivity,
                                           onCredentialsReceived: (CharArray) -> Unit) {
            onCredentialsReceived("1234".toCharArray())
        }

        override fun getInputStream(context: Context, identifier: String): FileInputStream {
            val file = File(context.filesDir, identifier)
            return file.inputStream()
        }

        override fun getOutputStream(context: Context, identifier: String): FileOutputStream {
            val file = File(context.filesDir, identifier)
            return file.outputStream()
        }

        override fun getKeystoreType(): String {
            return "BKS"
        }
    }
}