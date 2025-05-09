/*
 * Copyright (c) 2022 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import android.content.Intent
import android.net.Uri
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.forgerock.android.auth.callback.NameCallback
import org.forgerock.android.auth.callback.PasswordCallback
import org.forgerock.android.auth.storage.Storage
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.HttpURLConnection
import java.util.concurrent.CountDownLatch

@RunWith(RobolectricTestRunner::class)
class SSOBroadcastReceiverIntegrationTests: BaseTest() {

    private lateinit var ssoTokenStorage: Storage<SSOToken>
    private lateinit var cookiesStorage: Storage<Collection<String>>
    private lateinit var storage: Storage<AccessToken>

    @Before
    fun setUpStorage() {
        ssoTokenStorage = sharedPreferencesStorage<SSOToken>(context = context,
            filename = "ssotoken",
            key = "ssotoken", cacheable = false)
        cookiesStorage = sharedPreferencesStorage<Collection<String>>(context = context,
            filename = "cookies",
            key = "cookies", cacheable = false)
        storage = MemoryStorage()
    }

    @After
    fun cleanUp() {
        ssoTokenStorage.delete()
        cookiesStorage.delete()
        storage.delete()
    }


    private fun findRequest(path: String, vararg recordedRequests: RecordedRequest): RecordedRequest {
        for (r: RecordedRequest in recordedRequests) {
            if (r.path!!.startsWith(path)) {
                return r
            }
        }
        throw IllegalArgumentException()
    }

    private fun login() {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)
        Config.getInstance().oidcStorage = storage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
        Config.getInstance().url = url
        val nodeListenerFuture: NodeListenerFuture<FRUser?> =
            object : NodeListenerFuture<FRUser?>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback(NameCallback::class.java) != null) {
                        state.getCallback(NameCallback::class.java).setName("tester")
                        state.next(context, this)
                        return
                    }
                    if (state.getCallback(PasswordCallback::class.java) != null) {
                        state.getCallback(PasswordCallback::class.java)
                            .setPassword("password".toCharArray())
                        state.next(context, this)
                    }
                }
            }
        FRUser.login(context, nodeListenerFuture)
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()

        val request = server.takeRequest();

        val state = Uri.parse(request.path).getQueryParameter("state");
        server.enqueue(MockResponse()
            .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                    "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));

        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK)

        assertNotNull(nodeListenerFuture.get())
        assertNotNull(FRUser.getCurrentUser())
        val future = FRListenerFuture<UserInfo>()
        FRUser.getCurrentUser().getUserInfo(future)
        val userinfo = future.get()
        assertNotNull(userinfo)
    }

    @Test
    fun whenBroadcastReceiverGetsLogoutEventThenVerifyRevokeTokenInvoked() {
        login()

        val options = ConfigHelper.load(context, null).copy(Server(url, "root"))
        ConfigHelper.persist(context, options)

        val latch = CountDownLatch(1)
        val mockHttpDispatcher = MockHttpDispatcher(latch)
        server.dispatcher = mockHttpDispatcher

        val accessToken = FRUser.getCurrentUser().accessToken
        assertNotNull(FRUser.getCurrentUser())

        val config = ConfigHelper.getPersistedConfig(context, null)
        config.oidcStorage =  storage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
        val testObject = SSOBroadcastReceiver(config)
        val intent = Intent("org.forgerock.android.auth.broadcast.SSO_LOGOUT")
        intent.putExtra("BROADCAST_PACKAGE_KEY", "com.test.forgerock")
        testObject.onReceive(context, intent)

        val mockPackageManager = org.mockito.kotlin.mock<RecordedRequest>()

        latch.await()

        val revokeRequest: RecordedRequest = mockHttpDispatcher.list[0] ?: mockPackageManager //Post to oauth2/realms/root/token/revoke

        //revoke Refresh Token and SSO Token are performed async
        val refreshTokenRevoke: RecordedRequest =
            findRequest("/oauth2/realms/root/token/revoke", revokeRequest)
        val body = refreshTokenRevoke.body.readUtf8()
        assertTrue(body.contains("token"))
        assertTrue(body.contains("client_id"))
        assertTrue(body.contains(accessToken.refreshToken!!))
    }
}

private class MockHttpDispatcher(private val latch: CountDownLatch): Dispatcher() {
    val list = mutableListOf<RecordedRequest?>()
    override fun dispatch(request: RecordedRequest): MockResponse {
        if(request?.path == "/oauth2/realms/root/token/revoke") {
            list.add(request)
            latch.countDown()
        }
        return MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .setBody("{}")
    }

}

