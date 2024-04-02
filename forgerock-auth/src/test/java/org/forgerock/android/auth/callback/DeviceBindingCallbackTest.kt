/*
 * Copyright (c) 2022 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.forgerock.android.auth.CryptoKey
import org.forgerock.android.auth.DummyActivity
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.devicebind.ApplicationPinDeviceAuthenticator
import org.forgerock.android.auth.devicebind.BiometricAndDeviceCredential
import org.forgerock.android.auth.devicebind.BiometricBindingHandler
import org.forgerock.android.auth.devicebind.BiometricOnly
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.*
import org.forgerock.android.auth.devicebind.DeviceBindingException
import org.forgerock.android.auth.devicebind.DeviceBindingRepository
import org.forgerock.android.auth.devicebind.KeyPair
import org.forgerock.android.auth.devicebind.LocalDeviceBindingRepository
import org.forgerock.android.auth.devicebind.None
import org.forgerock.android.auth.devicebind.PinCollector
import org.forgerock.android.auth.devicebind.Prompt
import org.forgerock.android.auth.devicebind.Success
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

@RunWith(AndroidJUnit4::class)
class DeviceBindingCallbackTest {

    val context: Context = ApplicationProvider.getApplicationContext()

    private val encryptedPref = mock<DeviceBindingRepository>()
    private val deviceAuthenticator = mock<DeviceAuthenticator>()
    private val publicKey = mock<RSAPublicKey>()
    private val privateKey = mock<PrivateKey>()
    private val keyPair = KeyPair(publicKey, privateKey, "keyAlias")
    private val kid = "kid"
    private val userid = "id=demo,ou=user,dc=openam,dc=forgerock,dc=org"
    private val challenge = "CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM="

    @Test
    fun testValuesAreSetProperly() {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"APPLICATION_PIN\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}";
        val obj = JSONObject(rawContent)
        val testObject = DeviceBindingCallback(obj, 0)
        assertEquals(rawContent, testObject.getContent())
    }

    @Test
    fun testConfig() {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"APPLICATION_PIN\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}";
        val obj = JSONObject(rawContent)
        val testObject = DeviceBindingCallback(obj, 0)
        testObject.setDeviceName("jey")
        testObject.setJws("jws")
        testObject.setDeviceId("device_id")
        testObject.setClientError("Abort")
        val actualOutput = testObject.getContent()
        val expectedOut =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"APPLICATION_PIN\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"jws\"},{\"name\":\"IDToken1deviceName\",\"value\":\"jey\"},{\"name\":\"IDToken1deviceId\",\"value\":\"device_id\"},{\"name\":\"IDToken1clientError\",\"value\":\"Abort\"}]}";
        assertEquals(expectedOut, actualOutput)
    }

    @Test
    fun testSuccessPathForNoneType() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}";
        val encryptedPref = mock<DeviceBindingRepository>()
        val deviceAuthenticator = mock<None>()
        whenever(deviceAuthenticator.isSupported(any(), any())).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any(), any())).thenReturn(keyPair)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))
        whenever(deviceAuthenticator.sign(context, keyPair, null,
            kid,
            userid,
            challenge,
            getExpiration())).thenReturn("signedJWT")
        val testObject = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context,
            deviceAuthenticator = deviceAuthenticator,
            encryptedPreference = encryptedPref,
            "device_id")
        verify(deviceAuthenticator).setKey(any())

    }

    @Test
    fun testSuccessPathForBiometricType() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"BIOMETRIC_ONLY\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}";
        val deviceAuthenticator = mock<BiometricOnly>()
        whenever(deviceAuthenticator.type()).thenReturn(DeviceBindingAuthenticationType.BIOMETRIC_ONLY)
        whenever(deviceAuthenticator.isSupported(any(), any())).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any(), any())).thenReturn(keyPair)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))
        whenever(deviceAuthenticator.sign(context, keyPair, null,
            kid,
            userid,
            challenge,
            getExpiration())).thenReturn("signedJWT")

        val scenario: ActivityScenario<DummyActivity> =
            ActivityScenario.launch(DummyActivity::class.java)
        scenario.onActivity {
            InitProvider.setCurrentActivity(it)
        }

        val testObject = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context,
            deviceAuthenticator = deviceAuthenticator,
            encryptedPreference = encryptedPref,
            "device_id")
        val captor: KArgumentCaptor<Prompt> = argumentCaptor()
        verify(deviceAuthenticator).setKey(any())
        verify(deviceAuthenticator).prompt(captor.capture())
        assertTrue(captor.firstValue.title == "Authentication required")
        assertTrue(captor.firstValue.subtitle == "Cryptography device binding")
        assertTrue(captor.firstValue.description == "Please complete with biometric to proceed")
        verify(deviceAuthenticator).setBiometricHandler(any())
    }

    @Test
    fun testSuccessPathForBiometricAndCredentialType() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"BIOMETRIC_ALLOW_FALLBACK\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}";
        val scenario: ActivityScenario<DummyActivity> =
            ActivityScenario.launch(DummyActivity::class.java)
        scenario.onActivity {
            InitProvider.setCurrentActivity(it)
        }
        val deviceAuthenticator = mock<BiometricAndDeviceCredential>()
        whenever(deviceAuthenticator.type()).thenReturn(DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        whenever(deviceAuthenticator.isSupported(any(), any())).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any(), any())).thenReturn(keyPair)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))
        whenever(deviceAuthenticator.sign(context, keyPair, null,
            kid,
            userid,
            challenge,
            getExpiration())).thenReturn("signedJWT")

        val prompt = Prompt("test1", "test2", "test3")
        val testObject = DeviceBindingCallbackMockTest(rawContent)
        val captor: KArgumentCaptor<BiometricBindingHandler> = argumentCaptor()
        testObject.testExecute(context,
            deviceAuthenticator = deviceAuthenticator,
            encryptedPreference = encryptedPref,
            "device_id", prompt = prompt)
        verify(deviceAuthenticator).prompt(prompt)
        verify(deviceAuthenticator).setBiometricHandler(captor.capture())
        assertNotNull(captor.firstValue)
    }

    @Test
    fun testFailurePathForBiometricAbort(): Unit = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"APPLICATION_PIN\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}";
        val errorCode = -1
        val abort = Abort(code = errorCode)
        whenever(deviceAuthenticator.isSupported(any(), any())).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any(), any())).thenReturn(keyPair)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(abort)

        val testObject = DeviceBindingCallbackMockTest(rawContent)
        try {
            testObject.testExecute(context,
                deviceAuthenticator = deviceAuthenticator,
                encryptedPreference = encryptedPref,
                "device_id")
            fail()
        } catch (e: Exception) {
            assertTrue(e.message == abort.message)
            assertTrue(e is DeviceBindingException)
            val deviceBindException = e as DeviceBindingException
            assertTrue(deviceBindException.message == abort.message)
        }

        verify(encryptedPref, times(0)).delete(any()) //The key reference has not been created
        //Delete before and delete after when failed
        verify(deviceAuthenticator, times(2)).deleteKeys(any())
        verify(deviceAuthenticator, times(0)).sign(context,
            keyPair,
            null,
            kid,
            userid,
            challenge,
            getExpiration())
        verify(encryptedPref, times(0)).persist(any())
    }


    @Test
    fun testFailurePathForBiometricTimeout(): Unit = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"APPLICATION_PIN\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}";
        whenever(deviceAuthenticator.isSupported(any(), any())).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any(), any())).thenReturn(keyPair)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Timeout())

        val testObject = DeviceBindingCallbackMockTest(rawContent)
        try {
            testObject.testExecute(context,
                deviceAuthenticator = deviceAuthenticator,
                encryptedPreference = encryptedPref,
                "device_id")
            fail()
        } catch (e: Exception) {
            assertTrue(e.message == "Authentication Timeout")
            assertTrue(e is DeviceBindingException)
        }

        verify(encryptedPref, times(0)).delete(any())
        //Delete before and delete after when failed
        verify(deviceAuthenticator, times(2)).deleteKeys(any())
        verify(deviceAuthenticator, times(0)).sign(context,
            keyPair,
            null,
            kid,
            userid,
            challenge,
            getExpiration())
        verify(encryptedPref, times(0)).persist(any())
    }

    @Test
    fun testFailurePathForUnsupported(): Unit = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"APPLICATION_PIN\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}";
        whenever(deviceAuthenticator.isSupported(any(), any())).thenReturn(false)
        val testObject = DeviceBindingCallbackMockTest(rawContent)
        try {
            testObject.testExecute(context,
                deviceAuthenticator = deviceAuthenticator,
                encryptedPreference = encryptedPref,
                "device_id")
        } catch (e: Exception) {
            assertTrue(e.message == "Device not supported. Please verify the biometric or Pin settings")
            assertTrue(e is DeviceBindingException)
            val deviceBindException = e as DeviceBindingException
            assertTrue(deviceBindException.message == Unsupported().message)
        }

        val actualOutput = testObject.getContent()
        val expectedOut =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"APPLICATION_PIN\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"Unsupported\"}]}";
        assertEquals(expectedOut, actualOutput)

        verify(encryptedPref, times(0)).delete(any())
        verify(deviceAuthenticator, times(0)).deleteKeys(any())
        verify(deviceAuthenticator, times(0)).authenticate(any())
        verify(deviceAuthenticator, times(0)).sign(context,
            keyPair,
            null,
            kid,
            userid,
            challenge,
            getExpiration())
        verify(encryptedPref, times(0)).persist(any())

    }

    @Test
    fun testFailurePathForKeyGeneration(): Unit = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"APPLICATION_PIN\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}";
        whenever(deviceAuthenticator.isSupported(any(), any())).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any(),
            any())).thenThrow(NullPointerException::class.java)
        val testObject = DeviceBindingCallbackMockTest(rawContent)
        try {
            testObject.testExecute(context,
                deviceAuthenticator = deviceAuthenticator,
                encryptedPreference = encryptedPref,
                "device_id")
            fail()
        } catch (e: Exception) {
            assertTrue(e is DeviceBindingException)
            assertTrue(e.cause is java.lang.NullPointerException)
        }

        verify(encryptedPref, times(0)).delete(any())
        verify(deviceAuthenticator, times(2)).deleteKeys(any())
        verify(deviceAuthenticator, times(0)).authenticate(any())
        verify(deviceAuthenticator, times(0)).sign(context,
            keyPair,
            null,
            kid,
            userid,
            challenge,
            getExpiration())
        verify(encryptedPref, times(0)).persist(any())
    }

    @Test
    fun testWithApplicationPinBinding(): Unit = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=demo,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"demo\"},{\"name\":\"authenticationType\",\"value\":\"APPLICATION_PIN\"},{\"name\":\"challenge\",\"value\":\"CS3+g40VkHXx+dN7rpnJKhrEAvwZaYgbaXoEcpO5twM=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60},{\"name\":\"attestation\",\"value\":false}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}";

        val testObject = DeviceBindingCallbackMockTest(rawContent)
        val scenario: ActivityScenario<DummyActivity> =
            ActivityScenario.launch(DummyActivity::class.java)
        scenario.onActivity {
            InitProvider.setCurrentActivity(it)
        }
        val sharedPreferences = context.getSharedPreferences("TEST", Context.MODE_PRIVATE)
        val repository =
            LocalDeviceBindingRepository(context, sharedPreferences = sharedPreferences)

        testObject.testExecute(context, encryptedPreference = repository, deviceId = "deviceId")
    }

    fun getExpiration(): Date {
        val date = Calendar.getInstance();
        date.add(Calendar.SECOND, 60)
        return date.time;
    }
}


class DeviceBindingCallbackMockTest constructor(rawContent: String,
                                                jsonObject: JSONObject = JSONObject(rawContent),
                                                value: Int = 0) :
    DeviceBindingCallback(jsonObject, value) {

    private val cryptoKey = mock<CryptoKey>()

    suspend fun testExecute(context: Context,
                            deviceAuthenticator: DeviceAuthenticator,
                            encryptedPreference: DeviceBindingRepository,
                            deviceId: String,
                            prompt: Prompt? = null) {
        execute(context, deviceAuthenticator, encryptedPreference, deviceId, prompt)
    }

    suspend fun testExecute(context: Context,
                            encryptedPreference: DeviceBindingRepository,
                            deviceId: String,
                            prompt: Prompt? = null) {
        execute(context, deviceBindingRepository = encryptedPreference, deviceId = deviceId, prompt = prompt)
    }

    override fun getCryptoKey(): CryptoKey {
        return cryptoKey
    }

    override fun getDeviceAuthenticator(type: DeviceBindingAuthenticationType): DeviceAuthenticator {

        if (type == DeviceBindingAuthenticationType.APPLICATION_PIN) {
            val deviceAuthenticator =
                object : ApplicationPinDeviceAuthenticator(object : PinCollector {
                    override suspend fun collectPin(prompt: Prompt,
                                                    fragmentActivity: FragmentActivity): CharArray {
                        return "1234".toCharArray()
                    }
                }) {
                    var byteArrayOutputStream = ByteArrayOutputStream(1024)

                    override fun getInputStream(context: Context): InputStream {
                        return byteArrayOutputStream.toByteArray().inputStream();
                    }

                    override fun getOutputStream(context: Context): OutputStream {
                        return byteArrayOutputStream
                    }

                    override fun getKeystoreType(): String {
                        return "PKCS12"
                    }

                    override fun delete(context: Context) {
                        byteArrayOutputStream = ByteArrayOutputStream(1024)
                    }

                    override fun exist(context: Context): Boolean {
                        return byteArrayOutputStream.toByteArray().isNotEmpty()
                    }

                }

            return deviceAuthenticator
        }
        return super.getDeviceAuthenticator(type)
    }

}