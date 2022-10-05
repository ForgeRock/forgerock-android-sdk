/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.devicebind.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.CountDownLatch


@RunWith(AndroidJUnit4::class)
class DeviceBindingCallbackTest {

    val context: Context = ApplicationProvider.getApplicationContext()

    private val encryptedPref = mock<DeviceRepository>()
    private val keyAware = mock<DeviceAuthenticator>()
    private val publicKey = mock<RSAPublicKey>()
    private val privateKey = mock<PrivateKey>()
    private val keyPair = KeyPair(publicKey, privateKey, "keyAlias")
    private val kid = "kid"
    private val userid = "id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org"
    private val challenge = "uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw="

    @Test
    fun testValuesAreSetProperly() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"BIOMETRIC_ALLOW_FALLBACK\"},{\"name\":\"challenge\",\"value\":\"uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw=\"},{\"name\":\"title\",\"value\":\"jey\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val obj = JSONObject(rawContent)
        val testObject = DeviceBindingCallback(obj, 0)
        assertEquals(rawContent, testObject.getContent())
    }

    @Test
    fun testSetDeviceNameAndJWSAndClientError() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val obj = JSONObject(rawContent)
        val testObject = DeviceBindingCallback(obj, 0)
        testObject.setDeviceName("jey")
        testObject.setJws("andy")
        testObject.setDeviceId("device_id")
        testObject.setClientError("Abort")
        val actualOutput = testObject.getContent()
        val expectedOut = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":60}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"andy\"},{\"name\":\"IDToken1deviceName\",\"value\":\"jey\"},{\"name\":\"IDToken1deviceId\",\"value\":\"device_id\"},{\"name\":\"IDToken1clientError\",\"value\":\"Abort\"}]}"
        assertEquals(expectedOut, actualOutput)
    }

    @Test
    fun testSuccessPathForNoneType() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"challenge\",\"value\":\"uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw=\"},{\"name\":\"title\",\"value\":\"jey\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val encryptedPref = mock<DeviceRepository>()
        val authenticationLatch = CountDownLatch(1)
        whenever(keyAware.isSupported()).thenReturn(true)
        whenever(keyAware.generateKeys()).thenReturn(keyPair)
        whenever(keyAware.authenticate(eq(20), any())).thenAnswer {
            (it.arguments[1] as (DeviceBindingStatus) -> Unit).invoke(Success)
        }
        whenever(keyAware.sign(keyPair, kid, userid, challenge)).thenReturn("signedJWT")
        whenever(encryptedPref.persist(userid, "keyAlias", "", DeviceBindingAuthenticationType.NONE)).thenReturn(kid)

        var success = false
        val listener = object: FRListener<Void> {
            override fun onSuccess(result: Void?) {
                success = true
                authenticationLatch.countDown()
            }

            override fun onException(e: Exception?) {
                success = false
                fail()
            }
        }


        val testObject: DeviceBindingCallbackMockTest = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context, listener, authInterface = keyAware, encryptedPreference = encryptedPref, "device_id")
        authenticationLatch.await()
        assertTrue(success)
    }

    @Test
    fun testSuccessPathForBiometricType() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"BIOMETRIC_ONLY\"},{\"name\":\"challenge\",\"value\":\"uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw=\"},{\"name\":\"title\",\"value\":\"jey\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        whenever(keyAware.isSupported()).thenReturn(true)
        whenever(keyAware.generateKeys()).thenReturn(keyPair)
        whenever(keyAware.authenticate(eq(20), any())).thenAnswer {
            (it.arguments[1] as (DeviceBindingStatus) -> Unit).invoke(Success)
        }
        whenever(keyAware.sign(keyPair, kid, userid, challenge)).thenReturn("signedJWT")
        whenever(encryptedPref.persist(userid, "jey", "keyAlias", DeviceBindingAuthenticationType.BIOMETRIC_ONLY)).thenReturn(kid)

        val authenticationLatch = CountDownLatch(1)

        var success = false
        val listener = object: FRListener<Void> {
            override fun onSuccess(result: Void?) {
                success = true
                authenticationLatch.countDown()
            }

            override fun onException(e: Exception?) {
                success = false
                fail()
            }
        }
        val testObject: DeviceBindingCallbackMockTest = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context, listener, authInterface = keyAware, encryptedPreference = encryptedPref, "device_id")
        authenticationLatch.await()
        assertTrue(success)
    }

    @Test
    fun testSuccessPathForBiometricAndCredentialType() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"}, {\"name\":\"username\",\"value\":\"jey\"}, {\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        whenever(keyAware.isSupported()).thenReturn(true)
        whenever(keyAware.generateKeys()).thenReturn(keyPair)
        whenever(keyAware.authenticate(eq(20), any())).thenAnswer {
            (it.arguments[1] as (DeviceBindingStatus) -> Unit).invoke(Success)
        }
        whenever(keyAware.sign(keyPair, kid, userid, challenge)).thenReturn("signedJWT")
        whenever(encryptedPref.persist(userid, "jey","keyAlias", DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)).thenReturn(kid)

        val authenticationLatch = CountDownLatch(1)


        var success = false
        val listener = object: FRListener<Void> {
            override fun onSuccess(result: Void?) {
                success = true
                authenticationLatch.countDown()
            }

            override fun onException(e: Exception?) {
                success = false
                fail()
            }
        }

        val testObject: DeviceBindingCallbackMockTest = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context, listener, authInterface = keyAware, encryptedPreference = encryptedPref, "device_id")

        authenticationLatch.await()
        assertTrue(success)
    }

    @Test
    fun testFailurePathForBiometricAbort() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val errorCode = -1
        val abort = Abort(code = errorCode)
        whenever(keyAware.isSupported()).thenReturn(true)
        whenever(keyAware.generateKeys()).thenReturn(keyPair)
        whenever(keyAware.authenticate(eq(20), any())).thenAnswer {
            (it.arguments[1] as (DeviceBindingStatus) -> Unit).invoke(abort)
        }
        whenever(keyAware.sign(keyPair, kid, userid, challenge)).thenReturn("signedJWT")
        whenever(encryptedPref.persist(userid, "jey","keyAlias", DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)).thenReturn(kid)

        val authenticationLatch = CountDownLatch(1)

        var failed = false
        var exception: Exception? = null
        val listener = object: FRListener<Void> {
            override fun onSuccess(result: Void?) {
                failed = false
                fail()
            }

            override fun onException(e: Exception?) {
                exception = e
                failed = true
                authenticationLatch.countDown()
            }
        }
        val testObject: DeviceBindingCallbackMockTest = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context, listener, authInterface = keyAware, encryptedPreference = encryptedPref, "device_id")

        authenticationLatch.await()
        assertTrue(failed)
        assertTrue(exception?.message == abort.message)
        assertTrue(exception is DeviceBindingException)
        val deviceBindException = exception as DeviceBindingException
        assertTrue(deviceBindException.message ==  abort.message)
        verify(keyAware, times(0)).sign(keyPair, kid, userid, challenge)
        verify(encryptedPref, times(0)).persist(userid, "jey","keyAlias", DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
    }


    @Test
    fun testFailurePathForBiometricTimeout() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        whenever(keyAware.isSupported()).thenReturn(true)
        whenever(keyAware.generateKeys()).thenReturn(keyPair)
        whenever(keyAware.authenticate(eq(20), any())).thenAnswer {
            (it.arguments[1] as (DeviceBindingStatus) -> Unit).invoke(Timeout())
        }
        whenever(keyAware.sign(keyPair, kid, userid, challenge)).thenReturn("signedJWT")
        whenever(encryptedPref.persist(userid, "jey","keyAlias", DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)).thenReturn(kid)

        val authenticationLatch = CountDownLatch(1)

        var failed = false
        var exception: Exception? = null
        val listener = object: FRListener<Void> {
            override fun onSuccess(result: Void?) {
                failed = false
                fail()
            }

            override fun onException(e: Exception?) {
                exception = e
                failed = true
                authenticationLatch.countDown()
            }
        }

        val testObject: DeviceBindingCallbackMockTest = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context, listener, authInterface = keyAware, encryptedPreference = encryptedPref, "device_id")

        authenticationLatch.await()
        assertTrue(failed)
        assertTrue(exception?.message == "Biometric Timeout")
        assertTrue(exception is DeviceBindingException)
        verify(keyAware, times(0)).sign(keyPair, kid, userid, challenge)
        verify(encryptedPref, times(0)).persist(userid, "jey","keyAlias", DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
    }

    @Test
    fun testFailurePathForUnsupported() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        whenever(keyAware.isSupported()).thenReturn(false)

        val authenticationLatch = CountDownLatch(1)

        var failed = false
        var exception: Exception? = null
        val listener = object: FRListener<Void> {
            override fun onSuccess(result: Void?) {
                failed = false
                fail()
            }

            override fun onException(e: Exception?) {
                exception = e
                failed = true
                authenticationLatch.countDown()
            }
        }
        val testObject: DeviceBindingCallbackMockTest = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context, listener, authInterface = keyAware, encryptedPreference = encryptedPref, "device_id")

        authenticationLatch.await()
        assertTrue(failed)

        assertTrue(exception?.message == "Device not supported. Please verify the biometric or Pin settings")
        assertTrue(exception is DeviceBindingException)

        val deviceBindException = exception as DeviceBindingException
        assertTrue(deviceBindException.message == Unsupported().message)

        val actualOutput = testObject.getContent()
        val expectedOut = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"Unsupported\"}]}"
        assertEquals(expectedOut, actualOutput)

        verify(keyAware, times(0)).authenticate(eq(20), any())
        verify(keyAware, times(0)).sign(keyPair, kid, userid, challenge)
        verify(encryptedPref, times(0)).persist(userid, "jey","keyAlias", DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)

    }

    @Test
    fun testFailurePathForKeyGeneration() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"username\",\"value\":\"jey\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"eMr63WsBtwgZkIvqmrldSYxYqrwHntYAwzAUrBFWhiY=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1deviceId\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        whenever(keyAware.isSupported()).thenReturn(true)
        whenever(keyAware.generateKeys()).thenThrow(NullPointerException::class.java)
        whenever(keyAware.sign(keyPair, kid, userid, challenge)).thenReturn("signedJWT")
        whenever(encryptedPref.persist(userid, "jey","keyAlias", DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)).thenReturn(kid)

        val authenticationLatch = CountDownLatch(1)

        var failed = false
        var exception: Exception? = null
        val listener = object: FRListener<Void> {
            override fun onSuccess(result: Void?) {
                failed = false
                fail()
            }

            override fun onException(e: Exception?) {
                exception = e
                failed = true
                authenticationLatch.countDown()
            }
        }
        val testObject: DeviceBindingCallbackMockTest = DeviceBindingCallbackMockTest(rawContent)
        testObject.testExecute(context, listener, authInterface = keyAware, encryptedPreference = encryptedPref, "device_id")

        authenticationLatch.await()
        assertTrue(failed)
        assertTrue(exception is DeviceBindingException)
        assertTrue(exception?.message == "Failed to generate keypair or sign the transaction")
        assertNotNull(exception)
        verify(keyAware, times(0)).authenticate(eq(20), any())
        verify(keyAware, times(0)).sign(keyPair, kid, userid, challenge)
        verify(encryptedPref, times(0)).persist(userid, "jey","keyAlias", DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
    }
}


class DeviceBindingCallbackMockTest constructor(rawContent: String, jsonObject: JSONObject = JSONObject(rawContent), value: Int = 0): DeviceBindingCallback(jsonObject, value) {

     fun testExecute(
         context: Context,
         listener: FRListener<Void>,
         authInterface: DeviceAuthenticator,
         encryptedPreference: DeviceRepository,
         deviceId: String
    ) {
        execute(context, listener, authInterface, encryptedPreference, deviceId)
    }

    override fun execute(
        context: Context,
        listener: FRListener<Void>,
        authInterface: DeviceAuthenticator,
        encryptedPreference: DeviceRepository,
        deviceId: String
    ) {
        super.execute(context, listener, authInterface, encryptedPreference, deviceId)
    }
}