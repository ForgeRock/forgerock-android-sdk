/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.OkHttpClientProvider.getInstance
import org.forgerock.android.auth.SecureCookieJar.Companion.builder
import org.forgerock.android.auth.storage.Storage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.util.concurrent.ExecutionException

@RunWith(AndroidJUnit4::class)
class PersistentCookieTest : BaseTest() {

    private lateinit var ssoTokenStorage: Storage<SSOToken>
    private lateinit var cookiesStorage: Storage<Collection<String>>

    @Before
    @Throws(Exception::class)
    fun setUp() {
        //Since the application context changed for each test, we cannot cache the storage in SecureCookieJar.
        getInstance().clear()
        ssoTokenStorage = sharedPreferencesStorage<SSOToken>(context = context,
            filename = "ssotoken",
            key = "ssotoken", cacheable = false)
        cookiesStorage = sharedPreferencesStorage<Collection<String>>(context = context,
            filename = "cookies",
            key = "cookies", cacheable = false)
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage

    }

    @After
    fun cleanUp() {
        ssoTokenStorage.delete()
        cookiesStorage.delete()
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun persistCookieHappyPathTest() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .addHeader("Set-Cookie",
                "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
            .addHeader("Set-Cookie",
                "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
            .addHeader("Set-Cookie", "amlbcookie=01; Path=/; Domain=localhost")
            .setBody(getJson("/authTreeMockTest_Authenticate_success.json")))

        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                }
            }

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        Assertions.assertThat(nodeListenerFuture.get()).isInstanceOf(
            FRSession::class.java)
        Assertions.assertThat(FRSession.getCurrentSession()).isNotNull()
        Assertions.assertThat(FRSession.getCurrentSession().sessionToken).isNotNull()
        Assertions.assertThat(FRSession.getCurrentSession().sessionCookies).hasSize(3)

        nodeListenerFuture.reset()
        FRSession.authenticate(context, "Example", nodeListenerFuture)
        nodeListenerFuture.get()

        server.takeRequest()
        val rr = server.takeRequest() //Second request
        //The request should contains the received cookies
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["session-jwt"])
            .isEqualTo("session-jwt-cookie")
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["iPlanetDirectoryPro"])
            .isEqualTo("iPlanetDirectoryProCookie")
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun replaceCookie() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .addHeader("Set-Cookie",
                "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
            .addHeader("Set-Cookie",
                "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
            .addHeader("Set-Cookie", "amlbcookie=01; Path=/; Domain=localhost")
            .setBody(getJson("/authTreeMockTest_Authenticate_success.json")))

        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .addHeader("Set-Cookie",
                "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
            .addHeader("Set-Cookie",
                "session-jwt=session-jwt-cookie-new; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
            .addHeader("Set-Cookie", "amlbcookie=01; Path=/; Domain=localhost")
            .setBody(getJson("/authTreeMockTest_Authenticate_success.json")))

        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)


        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                }
            }

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        Assertions.assertThat(nodeListenerFuture.get()).isInstanceOf(
            FRSession::class.java)
        Assertions.assertThat(FRSession.getCurrentSession()).isNotNull()
        Assertions.assertThat(FRSession.getCurrentSession().sessionToken).isNotNull()

        //Second request with new cookie value
        nodeListenerFuture.reset()
        FRSession.authenticate(context, "Example", nodeListenerFuture)
        nodeListenerFuture.get()

        //Third request to check if the request has updated cookie
        nodeListenerFuture.reset()
        FRSession.authenticate(context, "Example", nodeListenerFuture)
        nodeListenerFuture.get()

        server.takeRequest() //first request
        server.takeRequest() //second request
        val rr = server.takeRequest() //Third request
        //The request should contains the received cookies
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["session-jwt"])
            .isEqualTo("session-jwt-cookie-new")
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["iPlanetDirectoryPro"])
            .isEqualTo("iPlanetDirectoryProCookie")
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun appendCookie() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .addHeader("Set-Cookie",
                "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
            .setBody(getJson("/authTreeMockTest_Authenticate_success.json")))

        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .addHeader("Set-Cookie",
                "session-jwt=session-jwt-cookie-new; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
            .setBody(getJson("/authTreeMockTest_Authenticate_success.json")))

        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)


        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                }
            }

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        Assertions.assertThat(nodeListenerFuture.get()).isInstanceOf(
            FRSession::class.java)
        Assertions.assertThat(FRSession.getCurrentSession()).isNotNull()
        Assertions.assertThat(FRSession.getCurrentSession().sessionToken).isNotNull()

        //Second request with new cookie
        nodeListenerFuture.reset()
        FRSession.authenticate(context, "Example", nodeListenerFuture)
        nodeListenerFuture.get()

        //Third request to check if the request has updated cookie
        nodeListenerFuture.reset()
        FRSession.authenticate(context, "Example", nodeListenerFuture)
        nodeListenerFuture.get()

        server.takeRequest() //first request
        server.takeRequest() //second request
        val rr = server.takeRequest() //Third request
        //The request should contains the received cookies
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["session-jwt"])
            .isEqualTo("session-jwt-cookie-new")
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["iPlanetDirectoryPro"])
            .isEqualTo("iPlanetDirectoryProCookie")
        Assertions.assertThat(Config.getInstance().singleSignOnManager.cookies).hasSize(2)
    }


    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun expiredCookie() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .addHeader("Set-Cookie",
                "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
            .addHeader("Set-Cookie",
                "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 1999 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
            .setBody(getJson("/authTreeMockTest_Authenticate_success.json")))

        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)


        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                }
            }

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        Assertions.assertThat(nodeListenerFuture.get()).isInstanceOf(
            FRSession::class.java)
        Assertions.assertThat(FRSession.getCurrentSession()).isNotNull()
        Assertions.assertThat(FRSession.getCurrentSession().sessionToken).isNotNull()

        nodeListenerFuture.reset()
        FRSession.authenticate(context, "Example", nodeListenerFuture)
        nodeListenerFuture.get()

        server.takeRequest()
        val rr = server.takeRequest() //Second request
        //The request should contains the received cookies
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["session-jwt"]).isNull()
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["iPlanetDirectoryPro"])
            .isEqualTo("iPlanetDirectoryProCookie")
        Assertions.assertThat(Config.getInstance().singleSignOnManager.cookies).hasSize(1)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun secureCookie() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .addHeader("Set-Cookie",
                "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
            .addHeader("Set-Cookie",
                "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2300 02:53:31 GMT; Path=/; Domain=localhost; Secure; HttpOnly")
            .setBody(getJson("/authTreeMockTest_Authenticate_success.json")))

        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)


        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                }
            }

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        Assertions.assertThat(nodeListenerFuture.get()).isInstanceOf(
            FRSession::class.java)
        Assertions.assertThat(FRSession.getCurrentSession()).isNotNull()
        Assertions.assertThat(FRSession.getCurrentSession().sessionToken).isNotNull()

        nodeListenerFuture.reset()
        FRSession.authenticate(context, "Example", nodeListenerFuture)
        nodeListenerFuture.get()

        server.takeRequest()
        val rr = server.takeRequest() //Second request
        //The request should contains the received cookies but not the one with secure, because we are using http, not https
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["session-jwt"]).isNull()
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["iPlanetDirectoryPro"])
            .isEqualTo("iPlanetDirectoryProCookie")
        //The cookie is stored, but not sent
        Assertions.assertThat(Config.getInstance().singleSignOnManager.cookies).hasSize(2)
    }


    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun cookieCache() {
        Config.getInstance().setCookieJar(builder()
            .context(context)
            .cacheIntervalMillis(1000L)
            .build())
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .addHeader("Set-Cookie",
                "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
            .addHeader("Set-Cookie",
                "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
            .setBody(getJson("/authTreeMockTest_Authenticate_success.json")))

        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                }
            }

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        Assertions.assertThat(nodeListenerFuture.get()).isInstanceOf(
            FRSession::class.java)

        //Second request with delete the cookies
        Config.getInstance().singleSignOnManager.clear()
        nodeListenerFuture.reset()
        FRSession.authenticate(context, "Example", nodeListenerFuture)
        nodeListenerFuture.get()

        server.takeRequest() //first request
        val rr = server.takeRequest() //Second request
        //The request should contains the received cookies
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["session-jwt"])
            .isEqualTo("session-jwt-cookie")
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["iPlanetDirectoryPro"])
            .isEqualTo("iPlanetDirectoryProCookie")
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun cookieCacheRemovedWithLogout() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .addHeader("Set-Cookie",
                "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
            .addHeader("Set-Cookie",
                "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
            .setBody(getJson("/authTreeMockTest_Authenticate_success.json")))

        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK)

        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                }
            }

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        Assertions.assertThat(nodeListenerFuture.get()).isInstanceOf(
            FRSession::class.java)

        FRSession.getCurrentSession().logout()

        //Second request with delete the cookies
        nodeListenerFuture.reset()
        FRSession.authenticate(context, "Example", nodeListenerFuture)
        nodeListenerFuture.get()

        server.takeRequest() //first request
        server.takeRequest() //logout request
        val rr = server.takeRequest() //Second request
        //The request should contains the received cookies
        Assertions.assertThat(rr.getHeader("Cookie")).isNull()
    }

    private fun toMap(cookieStr: String?): Map<String, String>? {
        if (cookieStr == null) {
            return null
        }
        val cookies = cookieStr.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val result: MutableMap<String, String> = HashMap()
        for (cookie in cookies) {
            val pair = cookie.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            result[pair[0].trim { it <= ' ' }] = pair[1].trim { it <= ' ' }
        }
        return result
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun persistWithDifferentDomain() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .addHeader("Set-Cookie",
                "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=test")
            .addHeader("Set-Cookie",
                "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
            .setBody(getJson("/authTreeMockTest_Authenticate_success.json")))

        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
                override fun onCallbackReceived(state: Node) {
                }
            }

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        Assertions.assertThat(nodeListenerFuture.get()).isInstanceOf(
            FRSession::class.java)
        Assertions.assertThat(FRSession.getCurrentSession()).isNotNull()
        Assertions.assertThat(FRSession.getCurrentSession().sessionToken).isNotNull()

        nodeListenerFuture.reset()
        FRSession.authenticate(context, "Example", nodeListenerFuture)
        nodeListenerFuture.get()

        server.takeRequest()
        val rr = server.takeRequest() //Second request

        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["iPlanetDirectoryPro"])
            .isNull() //Not the same domain
        Assertions.assertThat(toMap(rr.getHeader("Cookie"))!!["session-jwt"])
            .isEqualTo("session-jwt-cookie")
    }

    companion object {
        private const val DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest"
    }
}
