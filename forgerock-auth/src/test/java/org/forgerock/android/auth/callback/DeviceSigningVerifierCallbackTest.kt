/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
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
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import org.forgerock.android.auth.devicebind.DeviceBindFragment
import org.forgerock.android.auth.devicebind.KeyPair
import org.forgerock.android.auth.devicebind.MultipleKeysFound
import org.forgerock.android.auth.devicebind.NoKeysFound
import org.forgerock.android.auth.devicebind.SingleKeyFound
import org.forgerock.android.auth.devicebind.Success
import org.forgerock.android.auth.devicebind.UserKey
import org.forgerock.android.auth.devicebind.UserKeyService
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import java.util.concurrent.CountDownLatch

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
        val authenticationLatch = CountDownLatch(1)
        val userKey =
            UserKey("jey", "jey", "kid", DeviceBindingAuthenticationType.NONE, "jeyKeyAlias")
        whenever(userKeyService.getKeyStatus("jey")).thenReturn(SingleKeyFound(userKey))
        whenever(deviceAuthenticator.isSupported(context)).thenReturn(true)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))
        whenever(deviceAuthenticator.sign(userKey,
            keyPair.privateKey,
            "zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=",
            getExpiration())).thenReturn("jws")
        var success = false
        val listener = object : FRListener<Void> {
            override fun onSuccess(result: Void?) {
                success = true
                authenticationLatch.countDown()
            }

            override fun onException(e: Exception?) {
                success = false
                fail()
            }
        }

        val testObject: DeviceSigningVerifierCallbackMock =
            DeviceSigningVerifierCallbackMock(rawContent)
        testObject.executeAuthenticate(context, userKey, listener, deviceAuthenticator)
        authenticationLatch.await()
        assertTrue(success)
    }

    @Test
    fun testSuccessPathForMultipleKeyFound() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceSigningVerifierCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"jey\"},{\"name\":\"challenge\",\"value\":\"zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val mockFragmentActivity = mock<FragmentActivity>()
        val fragment = mock<DeviceBindFragment>()
        val userKeyService = mock<UserKeyService>()
        val authenticationLatch = CountDownLatch(1)
        val userKey =
            UserKey("jey", "jey", "kid", DeviceBindingAuthenticationType.NONE, "jeyKeyAlias")
        val userKey1 =
            UserKey("andy", "andy", "kid", DeviceBindingAuthenticationType.NONE, "jeyKeyAlias")

        val testObject: DeviceSigningVerifierCallbackMock =
            DeviceSigningVerifierCallbackMock(rawContent)

        whenever(userKeyService.getKeyStatus("jey")).thenReturn(MultipleKeysFound(mutableListOf(
            userKey,
            userKey1)))
        whenever(deviceAuthenticator.isSupported(context)).thenReturn(true)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))

        whenever(deviceAuthenticator.sign(userKey,
            keyPair.privateKey,
            "zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=",
            getExpiration())).thenReturn("jws")
        var success = false
        val listener = object : FRListener<Void> {
            override fun onSuccess(result: Void?) {
                success = true
                authenticationLatch.countDown()
            }

            override fun onException(e: Exception?) {
                success = false
                fail()
            }
        }

        var key = testObject.executeGetUserKey(mockFragmentActivity, userKeyService)
        testObject.executeAuthenticate(context, key, listener, deviceAuthenticator)

        authenticationLatch.await()
        assertTrue(success)
    }


    @Test
    fun testNoKeyFound() = runBlocking {
        val rawContent =
            "{\"type\":\"DeviceSigningVerifierCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"jey\"},{\"name\":\"challenge\",\"value\":\"zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=\"},{\"name\":\"title\",\"value\":\"Authentication required\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val authenticationLatch = CountDownLatch(1)
        val userKey =
            UserKey("jey", "jey", "kid", DeviceBindingAuthenticationType.NONE, "jeyKeyAlias")
        whenever(userKeyService.getKeyStatus("jey")).thenReturn(NoKeysFound)
        whenever(deviceAuthenticator.isSupported(context)).thenReturn(true)
        whenever(deviceAuthenticator.authenticate(any())).thenReturn(Success(keyPair.privateKey))

        whenever(deviceAuthenticator.sign(userKey,
            keyPair.privateKey,
            "zYwKaKnqS2YzvhXSK+sFjC7FKBoprArqz6LpJ8qe9+g=",
            getExpiration())).thenReturn("jws")
        var exception = false
        val listener = object : FRListener<Void> {
            override fun onSuccess(result: Void?) {
                exception = false
                fail()

            }

            override fun onException(e: Exception?) {
                exception = true
                authenticationLatch.countDown()
            }
        }

        val testObject: DeviceSigningVerifierCallbackMock =
            DeviceSigningVerifierCallbackMock(rawContent)
        testObject.executeAllKey(context, userKeyService, listener)
        authenticationLatch.await()
        assertTrue(exception)
    }

    fun getExpiration(): Date {
        val date = Calendar.getInstance();
        date.add(Calendar.SECOND, 60)
        return date.time;
    }
}


class DeviceSigningVerifierCallbackMock constructor(rawContent: String,
                                                    jsonObject: JSONObject = JSONObject(rawContent),
                                                    value: Int = 0) :
    DeviceSigningVerifierCallback(jsonObject, value) {

    suspend fun executeAuthenticate(context: Context,
                                    userKey: UserKey,
                                    listener: FRListener<Void>,
                                    authInterface: DeviceAuthenticator) {
        authenticate(context, userKey, listener, authInterface)
    }

    suspend fun executeGetUserKey(activity: FragmentActivity,
                                  viewModel: UserKeyService) : UserKey {
        return getUserKey(activity, viewModel)
    }

    fun executeAllKey(context: Context,
                      userKeyService: UserKeyService,
                      listener: FRListener<Void>) {
        super.execute(context, userKeyService, listener)
    }

    override suspend fun getUserKey(activity: FragmentActivity,
                                    userKeyService: UserKeyService): UserKey {
        return UserKey("jey", "jey", "kid", DeviceBindingAuthenticationType.NONE, "jeyKeyAlias")

    }

}