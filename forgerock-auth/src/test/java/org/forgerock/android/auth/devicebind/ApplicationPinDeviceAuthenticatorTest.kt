package org.forgerock.android.auth.devicebind

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.forgerock.android.auth.InitProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ApplicationPinDeviceAuthenticatorTest {

    private val activity = mock<FragmentActivity>()
    private val keyAware = KeyAware("bob")

    @Before
    fun setUp() {
        InitProvider.setCurrentActivity(activity)
        val keystoreFile = File(activity.filesDir, keyAware.key)
        if (keystoreFile.exists()) {
            keystoreFile.delete()
        }
    }

    @After
    fun tearDown() {
        InitProvider.setCurrentActivity(null);
        val keystoreFile = File(activity.filesDir, keyAware.key)
        if (keystoreFile.exists()) {
            keystoreFile.delete()
        }
    }

    private fun generateKeys() {
        val authenticator = getApplicationPinDeviceAuthenticator()
        authenticator.generateKeys {}
    }

    @Test
    fun testIsSupported() {
        val authenticator = getApplicationPinDeviceAuthenticator()
        assertThat(authenticator.isSupported()).isTrue()
    }

    @Test
    fun testGenerateKeys() {
        val authenticator = getApplicationPinDeviceAuthenticator()
        authenticator.generateKeys {
            assertThat(it).isNotNull
            assertThat(it.keyAlias).isEqualTo(keyAware.key + "_PIN")
            val keystoreFile = File(activity.filesDir, keyAware.key)
            assertThat(keystoreFile.exists()).isTrue()
        }
        //Test pin is cached for 1 sec
        assertThat(authenticator.pinRef.get()).isNotNull()
        Thread.sleep(1000)
        assertThat(authenticator.pinRef.get()).isNull()
    }

    @Test
    fun testSuccessAuthenticated() {
        generateKeys()
        val authenticator = getApplicationPinDeviceAuthenticator()
        authenticator.generateKeys {
            val keyPair = it
            authenticator.authenticate(60) {
                assertThat(it).isEqualTo(Success(keyPair.privateKey))
                assertThat(authenticator.pinRef.get()).isNull()
            }
        }
    }

    @Test
    fun testUnRegister() {
        val authenticator = getApplicationPinDeviceAuthenticator()
        authenticator.authenticate(60) {
            assertThat(it).isEqualTo(UnRegister())
        }
    }

    //Provide wrong pin
    @Test
    fun testUnAuthorize() {
        generateKeys()
        val authenticator = object : NoEncryptionApplicationPinDeviceAuthenticator(activity) {
            override fun requestForCredentials(fragmentActivity: FragmentActivity,
                                               onCredentialsReceived: (CharArray) -> Unit) {
                onCredentialsReceived("invalidPin".toCharArray())
            }
        }
        authenticator.setKeyAware(keyAware)
        authenticator.authenticate(60) {
            assertThat(it).isEqualTo(UnAuthorize())
        }
    }

    @Test
    fun testTimeout() {
        generateKeys()
        val authenticator = getApplicationPinDeviceAuthenticator()
        authenticator.authenticate(-1) {
            assertThat(it).isEqualTo(Timeout())
        }
    }

    @Test
    fun testAbort() {
        generateKeys()
        val authenticator = object : NoEncryptionApplicationPinDeviceAuthenticator(activity) {
            override fun requestForCredentials(fragmentActivity: FragmentActivity,
                                               onCredentialsReceived: (CharArray) -> Unit) {
                onCredentialsReceived(CharArray(0))
            }
        }
        authenticator.setKeyAware(keyAware)
        authenticator.authenticate(60) {
            assertThat(it).isEqualTo(Abort())
        }
    }

    private fun getApplicationPinDeviceAuthenticator() : ApplicationPinDeviceAuthenticator {
        val authenticator = NoEncryptionApplicationPinDeviceAuthenticator(activity);
        authenticator.setKeyAware(keyAware)
        return authenticator
    }
}


open class NoEncryptionApplicationPinDeviceAuthenticator constructor(context: Context) :
    ApplicationPinDeviceAuthenticator(context) {
    override fun requestForCredentials(fragmentActivity: FragmentActivity,
                                       onCredentialsReceived: (CharArray) -> Unit) {
        onCredentialsReceived("1234".toCharArray())
    }

    override fun getKeystoreFileInputStream(context: Context, keyAlias: String): FileInputStream {
        val file = File(context.filesDir, keyAlias)
        return file.inputStream()
    }

    override fun getKeystoreFileOutputStream(context: Context, keyAlias: String): FileOutputStream {
        val file = File(context.filesDir, keyAlias)
        return file.outputStream()
    }

    override fun getKeystoreType(): String {
        return "BKS"
    }
}