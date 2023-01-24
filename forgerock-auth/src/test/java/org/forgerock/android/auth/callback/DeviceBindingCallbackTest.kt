/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
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
import org.forgerock.android.auth.devicebind.BiometricOnly
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.*
import org.forgerock.android.auth.devicebind.DeviceBindingException
import org.forgerock.android.auth.devicebind.DeviceRepository
import org.forgerock.android.auth.devicebind.KeyPair
import org.forgerock.android.auth.devicebind.None
import org.forgerock.android.auth.devicebind.PinCollector
import org.forgerock.android.auth.devicebind.Prompt
import org.forgerock.android.auth.devicebind.SharedPreferencesDeviceRepository
import org.forgerock.android.auth.devicebind.Success
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
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
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class DeviceBindingCallbackTest {

    val context: Context = ApplicationProvider.getApplicationContext()

    private val encryptedPref = mock<DeviceRepository>()
    private val deviceAuthenticator = mock<DeviceAuthenticator>()
    private val publicKey = mock<RSAPublicKey>()
    private val privateKey = mock<PrivateKey>()
    private val keyPair = KeyPair(publicKey, privateKey, "keyAlias")
    private val kid = "kid"
    private val userid = "id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org"
    private val challenge = "uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw="


    @Test
    fun testValuesAreSetProperly() {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"BIOMETRIC_ALLOW_FALLBACK\"},{\"name\":\"challenge\",\"value\":\"uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw=\"},{\"name\":\"title\",\"value\":\"jey\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val obj = JSONObject(rawContent)
        val testObject = DeviceBindingCallback(obj, 0)
        assertEquals(rawContent, testObject.getContent())
    }

    @Test
    fun testSetDeviceNameAndJWSAndClientError() {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val obj = JSONObject(rawContent)
        val testObject = DeviceBindingCallback(obj, 0)
        testObject.setDeviceName("jey")
        testObject.setJws("andy")
        testObject.setDeviceId("device_id")
        testObject.setClientError("Abort")
        val actualOutput = testObject.getContent()
        val expectedOut =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"andy\"},{\"name\":\"IDToken1deviceName\",\"value\":\"jey\"},{\"name\":\"IDToken1deviceId\",\"value\":\"device_id\"},{\"name\":\"IDToken1clientError\",\"value\":\"Abort\"}]}"
        assertEquals(expectedOut, actualOutput)
    }

    @Test
    fun testSuccessPathForNoneType() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"challenge\",\"value\":\"uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw=\"},{\"name\":\"title\",\"value\":\"jey\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val encryptedPref = mock<DeviceRepository>()
        val authenticationLatch = CountDownLatch(1)
        val deviceAuthenticator = mock<None>()
        whenever(deviceAuthenticator.isSupported(any())).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any())).thenReturn(keyPair)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))
        whenever(deviceAuthenticator.sign(keyPair,
            kid,
            userid,
            challenge,
            getExpiration())).thenReturn("signedJWT")
        whenever(encryptedPref.persist(userid,
            "keyAlias",
            "",
            DeviceBindingAuthenticationType.NONE)).thenReturn(kid)

        val testObject = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context,
            authInterface = deviceAuthenticator,
            encryptedPreference = encryptedPref,
            "device_id")
        verify(deviceAuthenticator).setKey(any())

    }


    @Test
    fun testSuccessPathForBiometricType() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"BIOMETRIC_ONLY\"},{\"name\":\"challenge\",\"value\":\"uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw=\"},{\"name\":\"title\",\"value\":\"jey\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val deviceAuthenticator = mock<BiometricOnly>()
        whenever(deviceAuthenticator.type()).thenReturn(DeviceBindingAuthenticationType.BIOMETRIC_ONLY)
        whenever(deviceAuthenticator.isSupported(any())).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any())).thenReturn(keyPair)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))
        whenever(deviceAuthenticator.sign(keyPair,
            kid,
            userid,
            challenge,
            getExpiration())).thenReturn("signedJWT")
        whenever(encryptedPref.persist(userid,
            "jey",
            "keyAlias",
            DeviceBindingAuthenticationType.BIOMETRIC_ONLY)).thenReturn(kid)

        val scenario: ActivityScenario<DummyActivity> =
            ActivityScenario.launch(DummyActivity::class.java)
        scenario.onActivity {
            InitProvider.setCurrentActivity(it)
        }

        val testObject = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context,
            authInterface = deviceAuthenticator,
            encryptedPreference = encryptedPref,
            "device_id")
        verify(deviceAuthenticator).setKey(any())
        verify(deviceAuthenticator).setBiometricHandler(any())
    }

    @Test
    fun testSuccessPathForBiometricAndCredentialType() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"}, {\"name\":\"username\",\"value\":\"jey\"}, {\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        whenever(deviceAuthenticator.isSupported(context)).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any())).thenReturn(keyPair)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))
        whenever(deviceAuthenticator.sign(keyPair,
            kid,
            userid,
            challenge,
            getExpiration())).thenReturn("signedJWT")
        whenever(encryptedPref.persist(userid,
            "jey",
            "keyAlias",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)).thenReturn(kid)

        val testObject = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context,
            authInterface = deviceAuthenticator,
            encryptedPreference = encryptedPref,
            "device_id")
    }

    @Test
    fun testFailurePathForBiometricAbort(): Unit = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val errorCode = -1
        val abort = Abort(code = errorCode)
        whenever(deviceAuthenticator.isSupported(any())).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any())).thenReturn(keyPair)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(abort)

        val testObject = DeviceBindingCallbackMockTest(rawContent)
        try {
            testObject.testExecute(context,
                authInterface = deviceAuthenticator,
                encryptedPreference = encryptedPref,
                "device_id")
            fail()
        } catch (e: Exception) {
            assertTrue(e.message == abort.message)
            assertTrue(e is DeviceBindingException)
            val deviceBindException = e as DeviceBindingException
            assertTrue(deviceBindException.message == abort.message)
        }

        verify(encryptedPref).delete(any())
        verify(deviceAuthenticator).deleteKeys(any())
        verify(deviceAuthenticator, times(0)).sign(keyPair, kid, userid, challenge, getExpiration())
        verify(encryptedPref, times(0)).persist(userid,
            "jey",
            "keyAlias",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
    }


    @Test
    fun testFailurePathForBiometricTimeout(): Unit = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        whenever(deviceAuthenticator.isSupported(any())).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any())).thenReturn(keyPair)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Timeout())

        val testObject = DeviceBindingCallbackMockTest(rawContent)
        try {
            testObject.testExecute(context,
                authInterface = deviceAuthenticator,
                encryptedPreference = encryptedPref,
                "device_id")
            fail()
        } catch (e: Exception) {
            assertTrue(e.message == "Authentication Timeout")
            assertTrue(e is DeviceBindingException)
        }

        verify(encryptedPref).delete(any())
        verify(deviceAuthenticator).deleteKeys(any())
        verify(deviceAuthenticator, times(0)).sign(keyPair, kid, userid, challenge, getExpiration())
        verify(encryptedPref, times(0)).persist(userid,
            "jey",
            "keyAlias",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
    }

    @Test
    fun testFailurePathForUnsupported(): Unit = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        whenever(deviceAuthenticator.isSupported(context)).thenReturn(false)
        val testObject = DeviceBindingCallbackMockTest(rawContent)
        try {
            testObject.testExecute(context,
                authInterface = deviceAuthenticator,
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
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"Unsupported\"}]}"
        assertEquals(expectedOut, actualOutput)

        verify(encryptedPref, times(0)).delete(any())
        verify(deviceAuthenticator, times(0)).deleteKeys(any())
        verify(deviceAuthenticator, times(0)).authenticate(any())
        verify(deviceAuthenticator, times(0)).sign(keyPair, kid, userid, challenge, getExpiration())
        verify(encryptedPref, times(0)).persist(userid,
            "jey",
            "keyAlias",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)

    }

    @Test
    fun testFailurePathForKeyGeneration(): Unit = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        whenever(deviceAuthenticator.isSupported(context)).thenReturn(true)
        whenever(deviceAuthenticator.generateKeys(any())).thenThrow(NullPointerException::class.java)

        val testObject = DeviceBindingCallbackMockTest(rawContent)
        try {
            testObject.testExecute(context,
                authInterface = deviceAuthenticator,
                encryptedPreference = encryptedPref,
                "device_id")
            fail()
        } catch (e: Exception) {
            assertTrue(e is DeviceBindingException)
            assertTrue(e.cause is java.lang.NullPointerException)
        }

        verify(encryptedPref, times(0)).delete(any())
        verify(deviceAuthenticator).deleteKeys(any())
        verify(deviceAuthenticator, times(0)).authenticate(any())
        verify(deviceAuthenticator, times(0)).sign(keyPair, kid, userid, challenge, getExpiration())
        verify(encryptedPref, times(0)).persist(userid,
            "jey",
            "keyAlias",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
    }

    @Test
    fun testWithApplicationPinBinding(): Unit = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"APPLICATION_PIN\"},{\"name\":\"challenge\",\"value\":\"uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw=\"},{\"name\":\"title\",\"value\":\"jey\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val testObject = DeviceBindingCallbackMockTest(rawContent)
        val scenario: ActivityScenario<DummyActivity> =
            ActivityScenario.launch(DummyActivity::class.java)
        scenario.onActivity {
            InitProvider.setCurrentActivity(it)
        }
        val sharedPreferences = context.getSharedPreferences("TEST", Context.MODE_PRIVATE)
        val repository =
            SharedPreferencesDeviceRepository(context, sharedPreferences = sharedPreferences)

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

    suspend fun testExecute(context: Context,
                            authInterface: DeviceAuthenticator,
                            encryptedPreference: DeviceRepository,
                            deviceId: String) {
        execute(context, authInterface, encryptedPreference, deviceId)
    }

    suspend fun testExecute(context: Context,
                            encryptedPreference: DeviceRepository,
                            deviceId: String) {
        execute(context, encryptedPreference = encryptedPreference, deviceId = deviceId)
    }


    override fun getDeviceAuthenticator(type: DeviceBindingAuthenticationType): DeviceAuthenticator {

        if (type == DeviceBindingAuthenticationType.APPLICATION_PIN) {
            val deviceAuthenticator = object : ApplicationPinDeviceAuthenticator(object : PinCollector {
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

            deviceAuthenticator.setKey(CryptoKey(userId))
            return deviceAuthenticator
        }
        return super.getDeviceAuthenticator(type)
    }
}