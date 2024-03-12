/*
 * Copyright (c) 2022 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import org.forgerock.android.auth.devicebind.DeviceBindFragment
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus
import org.forgerock.android.auth.devicebind.DeviceBindingException
import org.forgerock.android.auth.devicebind.KeyPair
import org.forgerock.android.auth.devicebind.MultipleKeysFound
import org.forgerock.android.auth.devicebind.NoKeysFound
import org.forgerock.android.auth.devicebind.Prompt
import org.forgerock.android.auth.devicebind.SingleKeyFound
import org.forgerock.android.auth.devicebind.Success
import org.forgerock.android.auth.devicebind.UserKey
import org.forgerock.android.auth.devicebind.UserKeySelector
import org.forgerock.android.auth.devicebind.UserKeyService
import org.forgerock.android.auth.devicebind.UserKeys
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

@RunWith(AndroidJUnit4::class)
class DeviceSigningVerifierCallbackTest {

    val context: Context = ApplicationProvider.getApplicationContext()
    private val userKeyService = mock<UserKeyService>()
    private val deviceAuthenticator = mock<DeviceAuthenticator>()
    private val publicKey = mock<RSAPublicKey>()
    private val privateKey = mock<PrivateKey>()
    private val keyPair = KeyPair(publicKey, privateKey, "keyAlias")


    @Test
    fun testValuesAreSetProperly() {
        val rawContent =
            "{\"type\":\"DeviceSigningVerifierCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"\"},{\"name\":\"challenge\",\"value\":\"zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":5}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val obj = JSONObject(rawContent)
        val testObject = DeviceSigningVerifierCallback(obj, 0)
        assertEquals(rawContent, testObject.getContent())
    }

    @Test
    fun testJWSAndClientError() {
        val rawContent =
            "{\"type\":\"DeviceSigningVerifierCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"\"},{\"name\":\"challenge\",\"value\":\"zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":5}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val obj = JSONObject(rawContent)
        val testObject = DeviceSigningVerifierCallback(obj, 0)
        testObject.setJws("andy")
        testObject.setClientError("Abort")
        val actualOutput = testObject.getContent()
        val expectedOut =
            "{\"type\":\"DeviceSigningVerifierCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"\"},{\"name\":\"challenge\",\"value\":\"zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":5}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"andy\"},{\"name\":\"IDToken1clientError\",\"value\":\"Abort\"}]}"
        assertEquals(expectedOut, actualOutput)
    }

    @Test
    fun testGetType() {
        val rawContent =
            "{\"type\":\"DeviceSigningVerifierCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"\"},{\"name\":\"challenge\",\"value\":\"zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":5}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val obj = JSONObject(rawContent)
        val testObject = DeviceSigningVerifierCallback(obj, 0)
        assertEquals("DeviceSigningVerifierCallback", testObject.type)
    }

    @Test
    fun testSuccessPathForSingeKeyFound() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceSigningVerifierCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"jey\"},{\"name\":\"challenge\",\"value\":\"zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val userKey =
            UserKey("id1", "jey", "jey", "kid", DeviceBindingAuthenticationType.NONE, System.currentTimeMillis())
        whenever(userKeyService.getKeyStatus("jey")).thenReturn(SingleKeyFound(userKey))
        whenever(deviceAuthenticator.isSupported(context)).thenReturn(true)
        whenever(deviceAuthenticator.validateCustomClaims(any())).thenReturn(true)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))
        whenever(deviceAuthenticator.sign(context, userKey,
            keyPair.privateKey,
            null,
            "zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=",
            getExpiration())).thenReturn("jws")

        val prompt = Prompt("test1", "test2", "test3")
        val testObject =
            DeviceSigningVerifierCallbackMock(rawContent)
        testObject.executeAuthenticate(context, userKey, deviceAuthenticator, prompt = prompt)
        verify(deviceAuthenticator).prompt(prompt)
    }

    @Test
    fun testSuccessPathForMultipleKeyFound() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceSigningVerifierCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"jey\"},{\"name\":\"challenge\",\"value\":\"zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val mockFragmentActivity = mock<FragmentActivity>()
        val fragment = mock<DeviceBindFragment>()
        val userKeyService = mock<UserKeyService>()
        val userKey =
            UserKey("id1","jey", "jey", "kid", DeviceBindingAuthenticationType.NONE, System.currentTimeMillis() )
        val userKey1 =
            UserKey("id2","andy", "andy", "kid", DeviceBindingAuthenticationType.NONE, System.currentTimeMillis())

        val testObject =
            DeviceSigningVerifierCallbackMock(rawContent)

        whenever(userKeyService.getKeyStatus("jey")).thenReturn(MultipleKeysFound(mutableListOf(
            userKey,
            userKey1)))
        whenever(deviceAuthenticator.validateCustomClaims(any())).thenReturn(true)
        whenever(deviceAuthenticator.isSupported(context)).thenReturn(true)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))

        whenever(deviceAuthenticator.sign(context, userKey,
            keyPair.privateKey,
            null,
            "zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=",
            getExpiration())).thenReturn("jws")
        val key = UserKey("id1", "jey", "jey", "kid", DeviceBindingAuthenticationType.NONE, System.currentTimeMillis())
        testObject.executeAuthenticate(context, key, deviceAuthenticator)

        val captor: KArgumentCaptor<Prompt> = argumentCaptor()
        verify(deviceAuthenticator).prompt(captor.capture())
        Assert.assertTrue(captor.firstValue.title == "Authentication required")
        Assert.assertTrue(captor.firstValue.subtitle == "Cryptography device binding")
        Assert.assertTrue(captor.firstValue.description == "Please complete with biometric to proceed")

    }

    @Test(expected = DeviceBindingException::class )
    fun testNoKeyFound() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceSigningVerifierCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"jey\"},{\"name\":\"challenge\",\"value\":\"zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val userKey =
            UserKey("id1", "jey", "jey", "kid", DeviceBindingAuthenticationType.NONE, System.currentTimeMillis())
        whenever(userKeyService.getKeyStatus("jey")).thenReturn(NoKeysFound)
        whenever(deviceAuthenticator.isSupported(context)).thenReturn(true)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))

        whenever(deviceAuthenticator.sign(context, userKey,
            keyPair.privateKey,
            null,
            "zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=",
            getExpiration())).thenReturn("jws")

        val testObject =
            DeviceSigningVerifierCallbackMock(rawContent)
        testObject.executeAllKey(context, userKeyService) { deviceAuthenticator }
    }

    fun getExpiration(): Date {
        val date = Calendar.getInstance();
        date.add(Calendar.SECOND, 60)
        return date.time;
    }

    fun testSignForForValidClaims() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceSigningVerifierCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"jey\"},{\"name\":\"challenge\",\"value\":\"zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val userKey =
            UserKey("id1", "jey", "jey", "kid", DeviceBindingAuthenticationType.NONE, System.currentTimeMillis())
        whenever(userKeyService.getKeyStatus("jey")).thenReturn(SingleKeyFound(userKey))
        whenever(deviceAuthenticator.isSupported(context)).thenReturn(true)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))
        whenever(deviceAuthenticator.validateCustomClaims(any())).thenReturn(true)
        whenever(deviceAuthenticator.sign(context, userKey,
            keyPair.privateKey,
            null,
            "zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=",
            getExpiration())).thenReturn("jws")

        val testObject =
            DeviceSigningVerifierCallbackMock(rawContent)
        testObject.executeAuthenticate(context, userKey, deviceAuthenticator)
    }


    fun testSignForForInvalidClaims() = runBlocking {
        val errorCode = -1
        val invalidCustomClaims = DeviceBindingErrorStatus.InvalidCustomClaims(code = errorCode)
        val rawContent =
            "{\"type\":\"DeviceSigningVerifierCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"jey\"},{\"name\":\"challenge\",\"value\":\"zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val userKey =
            UserKey("id1", "jey", "jey", "kid", DeviceBindingAuthenticationType.NONE, System.currentTimeMillis())
        whenever(userKeyService.getKeyStatus("jey")).thenReturn(SingleKeyFound(userKey))
        whenever(deviceAuthenticator.isSupported(context)).thenReturn(true)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))
        whenever(deviceAuthenticator.validateCustomClaims(any())).thenReturn(false)
        whenever(deviceAuthenticator.sign(context, userKey,
            keyPair.privateKey,
            null,
            "zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=",
            getExpiration())).thenReturn("jws")

        val testObject =
            DeviceSigningVerifierCallbackMock(rawContent)
        try {
            testObject.executeAuthenticate(context, userKey, deviceAuthenticator)
            Assert.fail()
        } catch (e: Exception) {
            Assert.assertTrue(e.message == invalidCustomClaims.message)
            Assert.assertTrue(e is DeviceBindingException)
            val deviceBindException = e as DeviceBindingException
            Assert.assertTrue(deviceBindException.message == invalidCustomClaims.message)
        }
    }
}


class DeviceSigningVerifierCallbackMock constructor(rawContent: String,
                                                    jsonObject: JSONObject = JSONObject(rawContent),
                                                    value: Int = 0) :
    DeviceSigningVerifierCallback(jsonObject, value) {

    suspend fun executeAuthenticate(context: Context,
                                    userKey: UserKey,
                                    authInterface: DeviceAuthenticator,
                                    prompt: Prompt? = null) {
        authenticate(context, userKey, authInterface, prompt = prompt)
    }

    suspend fun executeAllKey(context: Context,
                              userKeyService: UserKeyService, prompt: Prompt? = null, authenticator: (DeviceBindingAuthenticationType) -> DeviceAuthenticator) {
        super.execute(context, userKeyService, userKeySelector = object : UserKeySelector {
            override suspend fun selectUserKey(userKeys: UserKeys,
                                               fragmentActivity: FragmentActivity): UserKey {
                return UserKey("id1" , "jey", "jey", "kid", DeviceBindingAuthenticationType.NONE, System.currentTimeMillis())
            }
        }, deviceAuthenticator = authenticator, customClaims = emptyMap(), prompt = prompt)
    }
}