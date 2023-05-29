/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.okhttp.mockwebserver.MockResponse
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.callback.NameCallback
import org.forgerock.android.auth.callback.PasswordCallback
import org.json.JSONException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.text.ParseException
import java.util.concurrent.ExecutionException

@RunWith(AndroidJUnit4::class)
class FRLifeCycleListenerTest : BaseTest() {
    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        MalformedURLException::class,
        ParseException::class,
        JSONException::class)
    fun testListener() {
        val onSSOTokenUpdated = intArrayOf(0)
        val onCookiesUpdated = intArrayOf(0)
        val onLogout = intArrayOf(0)
        val lifecycleListener: FRLifecycleListener = object : FRLifecycleListener {
            override fun onSSOTokenUpdated(ssoToken: SSOToken) {
                Assertions.assertThat(ssoToken.value)
                    .isEqualTo("C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*")
                onSSOTokenUpdated[0]++
            }

            override fun onCookiesUpdated(cookies: Collection<String>) {
                onCookiesUpdated[0]++
            }

            override fun onLogout() {
                onLogout[0]++
            }
        }
        FRLifecycle.registerFRLifeCycleListener(lifecycleListener)
        authenticate()
        FRLifecycle.unregisterFRLifeCycleListener(lifecycleListener)

        //Assert that FRLifeCycleListener is invoked
        Assertions.assertThat(onSSOTokenUpdated[0]).isEqualTo(1)
        Assertions.assertThat(onCookiesUpdated[0]).isEqualTo(1)
        Assertions.assertThat(onLogout[0]).isEqualTo(1)
    }

    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        MalformedURLException::class,
        ParseException::class,
        JSONException::class)
    fun testUnRegister() {
        val onSSOTokenUpdated = intArrayOf(0)
        val onCookiesUpdated = intArrayOf(0)
        val onLogout = intArrayOf(0)
        val lifecycleListener: FRLifecycleListener = object : FRLifecycleListener {
            override fun onSSOTokenUpdated(ssoToken: SSOToken) {
                Assertions.assertThat(ssoToken.value)
                    .isEqualTo("C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*")
                onSSOTokenUpdated[0]++
            }

            override fun onCookiesUpdated(cookies: Collection<String>) {
                onCookiesUpdated[0]++
            }

            override fun onLogout() {
                onLogout[0]++
            }
        }
        FRLifecycle.registerFRLifeCycleListener(lifecycleListener)
        FRLifecycle.unregisterFRLifeCycleListener(lifecycleListener)
        authenticate()

        //Assert that FRLifeCycleListener is invoked
        Assertions.assertThat(onSSOTokenUpdated[0]).isEqualTo(0)
        Assertions.assertThat(onCookiesUpdated[0]).isEqualTo(0)
        Assertions.assertThat(onLogout[0]).isEqualTo(0)
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    private fun authenticate() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .addHeader("Set-Cookie",
                "iPlanetDirectoryPro=C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*; Path=/; Domain=localhost; HttpOnly")
            .setBody(getJson("/authTreeMockTest_Authenticate_success.json")))
        Config.getInstance().sharedPreferences = context.getSharedPreferences(
            DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE)
        Config.getInstance().ssoSharedPreferences = context.getSharedPreferences(
            DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE)
        Config.getInstance().url = url
        val nodeListenerFuture: NodeListenerFuture<FRUser> =
            object : NodeListenerFuture<FRUser>() {
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
        val request = server.takeRequest()
        val state = Uri.parse(request.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK)
        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())
        FRUser.getCurrentUser().logout()
    }

    companion object {
        private const val DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest"
        private const val DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOTokenManagerTest"
    }
}