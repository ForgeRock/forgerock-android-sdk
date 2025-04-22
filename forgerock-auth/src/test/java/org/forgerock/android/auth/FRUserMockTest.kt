/*
 * Copyright (c) 2019 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.res.Resources
import android.net.Uri
import android.util.Pair
import okhttp3.Cookie
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.callback.NameCallback
import org.forgerock.android.auth.callback.PasswordCallback
import org.forgerock.android.auth.callback.StringAttributeInputCallback
import org.forgerock.android.auth.callback.ValidatedPasswordCallback
import org.forgerock.android.auth.callback.ValidatedUsernameCallback
import org.forgerock.android.auth.exception.AlreadyAuthenticatedException
import org.forgerock.android.auth.exception.ApiException
import org.forgerock.android.auth.exception.AuthenticationException
import org.forgerock.android.auth.exception.AuthenticationRequiredException
import org.forgerock.android.auth.storage.Storage
import org.json.JSONException
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.lang.reflect.Field
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException

@RunWith(RobolectricTestRunner::class)
class FRUserMockTest : BaseTest() {
    @Mock
    var mockBroadcastModel: SSOBroadcastModel? = null
    private lateinit var ssoTokenStorage: Storage<SSOToken>
    private lateinit var cookiesStorage: Storage<Collection<String>>
    private lateinit var tokenStorage: Storage<AccessToken>

    @Before
    @Throws(Exception::class)
    fun setUp() {
        Config.getInstance().ssoBroadcastModel = mockBroadcastModel
        Mockito.`when`(mockBroadcastModel!!.isBroadcastEnabled()).thenReturn(true)
        ssoTokenStorage = sharedPreferencesStorage<SSOToken>(context = context,
            filename = "ssotoken",
            key = "ssotoken", cacheable = false)
        cookiesStorage = sharedPreferencesStorage<Collection<String>>(context = context,
            filename = "cookies",
            key = "cookies", cacheable = false)
        tokenStorage = Memory()
    }

    @After
    fun cleanUp() {
        ssoTokenStorage.delete()
        cookiesStorage.delete()
        tokenStorage.delete()
    }

    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        MalformedURLException::class,
        ParseException::class,
        JSONException::class)
    fun frUserHappyPath() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = Memory()
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
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
        val recordedRequest = server.takeRequest()
        val state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK)

        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        val future = FRListenerFuture<UserInfo>()
        FRUser.getCurrentUser().getUserInfo(future)
        val userinfo = future.get()

        Assert.assertEquals("sub", userinfo.sub)
        Assert.assertEquals("name", userinfo.name)
        Assert.assertEquals("given name", userinfo.givenName)
        Assert.assertEquals("family name", userinfo.familyName)
        Assert.assertEquals("middle name", userinfo.middleName)
        Assert.assertEquals("nick name", userinfo.nickName)
        Assert.assertEquals("preferred username", userinfo.preferredUsername)
        Assert.assertEquals(URL("http://profile"), userinfo.profile)
        Assert.assertEquals(URL("http://picture"), userinfo.picture)
        Assert.assertEquals(URL("http://website"), userinfo.website)
        Assert.assertEquals("test@email.com", userinfo.email)
        Assert.assertEquals(true, userinfo.emailVerified)
        Assert.assertEquals("male", userinfo.gender)
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        Assert.assertEquals(simpleDateFormat.parse("2008-01-30"), userinfo.birthDate)
        Assert.assertEquals("zoneinfo", userinfo.zoneInfo)
        Assert.assertEquals("locale", userinfo.locale)
        Assert.assertEquals("phone number", userinfo.phoneNumber)
        Assert.assertEquals(true, userinfo.phoneNumberVerified)
        Assert.assertEquals("800000", userinfo.updateAt.toString())
        Assert.assertEquals("formatted", userinfo.address.formatted)
        Assert.assertEquals("street address", userinfo.address.streetAddress)
        Assert.assertEquals("locality", userinfo.address.locality)
        Assert.assertEquals("region", userinfo.address.region)
        Assert.assertEquals("90210", userinfo.address.postalCode)
        Assert.assertEquals("US", userinfo.address.country)
        Assert.assertEquals(getJson("/userinfo_success.json"), userinfo.raw.toString(2))
    }

    @Ignore("For now not to cache userinfo in memory")
    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        MalformedURLException::class,
        ParseException::class,
        JSONException::class)
    fun userInfoIsCached() {
        frUserHappyPath()
        //No userinfo enqueued
        val future = FRListenerFuture<UserInfo>()
        FRUser.getCurrentUser().getUserInfo(future)
        future.get()
    }

    /**
     * Start -> Platform Username -> Platform Password -> Attribute Collector -> Create Object
     */
    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        JSONException::class,
        ParseException::class,
        MalformedURLException::class)
    fun frAuthRegistrationHappyPath() {
        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage

        Config.getInstance().url = url

        val nodeListenerFuture: NodeListenerFuture<FRUser> =
            object : NodeListenerFuture<FRUser>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback(ValidatedUsernameCallback::class.java) != null) {
                        state.getCallback(ValidatedUsernameCallback::class.java)
                            .setUsername("tester")
                        state.next(context, this)
                        return
                    }

                    if (state.getCallback(ValidatedPasswordCallback::class.java) != null) {
                        state.getCallback(ValidatedPasswordCallback::class.java)
                            .setPassword("password".toCharArray())
                        state.next(context, this)
                        return
                    }

                    val callbacks = state.callbacks
                    val email = (callbacks[0] as StringAttributeInputCallback)
                    val firstName = (callbacks[1] as StringAttributeInputCallback)
                    val lastName = (callbacks[2] as StringAttributeInputCallback)
                    email.value = "test@test.com"
                    firstName.value = "My First Name"
                    lastName.value = "My Last Name"
                    state.next(context, this)
                }
            }

        FRUser.register(context, nodeListenerFuture)
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        val recordedRequest = server.takeRequest()
        val state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK)

        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())
    }

    @Test
    @Throws(Exception::class)
    fun testAccessToken() {
        frUserHappyPath()
        val accessToken = FRUser.getCurrentUser().accessToken
        Assert.assertNotNull(accessToken.value)
    }

    @Test
    @Throws(Exception::class)
    fun testRevokeAccessToken() {
        frUserHappyPath()
        //revoke Access Token
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .setBody("{}"))

        val future = FRListenerFuture<Void>()
        Assert.assertNotNull(FRUser.getCurrentUser())
        //Check if the token exists
        Assert.assertTrue(Config.getInstance().tokenManager.hasToken())
        //Revoke the token
        FRUser.getCurrentUser().revokeAccessToken(future)
        try {
            future.get()
        } catch (e: ExecutionException) {
            //Timeout exception expected
            Assert.assertEquals("java.net.SocketTimeoutException: timeout", e.message)
        }
        //Check that the token has been cleared
        Assert.assertFalse(Config.getInstance().tokenManager.hasToken())
    }

    @Test
    @Throws(Exception::class)
    fun testAccessTokenAsync() {
        frUserHappyPath()
        val future = FRListenerFuture<AccessToken>()
        FRUser.getCurrentUser().getAccessToken(future)
        Assert.assertNotNull(future.get())
    }

    @Test
    @Throws(Exception::class)
    fun testRefreshTokenAsync() {
        frUserHappyPath()
        val future = FRListenerFuture<AccessToken>()
        FRUser.getCurrentUser().getAccessToken(future)
        Assert.assertNotNull(future.get())
        //server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));
        //For Asyn revoke
        enqueue("/authTreeMockTest_Authenticate_accessToken_no_RefreshToken_shortlife.json",
            HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_accessToken_no_RefreshToken_shortlife.json",
            HttpURLConnection.HTTP_OK)
        //For revoke existing Access Token
        val refreshTokenFuture = FRListenerFuture<AccessToken>()
        FRUser.getCurrentUser().refresh(refreshTokenFuture)
        server.takeRequest()
        Assert.assertNotEquals(future.get().expiresIn, refreshTokenFuture.get().expiresIn)
        Assert.assertNotEquals(future.get().value, refreshTokenFuture.get().value)
        Assert.assertNotEquals(future.get().idToken, refreshTokenFuture.get().idToken)
    }
    @Test(expected = AuthenticationRequiredException::class)
    @Throws(
        Throwable::class)
    fun testAccessTokenIsNullThrowException() {
        frUserHappyPath()
        val future = FRListenerFuture<AccessToken>()
        FRUser.getCurrentUser().getAccessToken(future)
        Assert.assertNotNull(future.get())
        // This will clear the token in the OIDC storage
        Config.getInstance().oidcStorage.delete()
        val refreshTokenFuture = FRListenerFuture<AccessToken>()
        FRUser.getCurrentUser().refresh(refreshTokenFuture)
        // verify the exception
        try {
            refreshTokenFuture.get()
            Assert.fail()
        } catch (e: ExecutionException) {
            Assert.assertTrue(e.message!!.contains("Access Token does not exists."))
            throw e.cause!!
        }
    }

    @Test(expected = AuthenticationRequiredException::class)
    @Throws(
        Throwable::class)
    fun testRefreshTokenIsNullThrowException() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
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
        val recordedRequest = server.takeRequest()
        val state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
       // AccessToken returned from the server is empty. The storage does not have refresh token.
        enqueue("/authTreeMockTest_Authenticate_no_accessToken_no_RefreshToken.json", HttpURLConnection.HTTP_OK)

        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        //Check RefreshToken State by sending future as a listener.
        val refreshTokenFuture = FRListenerFuture<AccessToken>()
        FRUser.getCurrentUser().refresh(refreshTokenFuture)
        // If the RefreshToken is null, the refresh token flow should throw an AuthenticationRequiredException.
        try {
            refreshTokenFuture.get()
            Assert.fail()
        } catch (e: ExecutionException) {
            Assert.assertTrue(e.message!!.contains("Refresh Token does not exists."))
            throw e.cause!!
        }
    }

    @Test(expected = AuthenticationException::class)
    @Throws(
        Throwable::class)
    fun testUserInfoFailedDueToTokenExpired() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage

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
        val recordedRequest = server.takeRequest()
        val state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        enqueue("/userinfo_failed.json", HttpURLConnection.HTTP_UNAUTHORIZED)


        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        val future = FRListenerFuture<UserInfo>()
        FRUser.getCurrentUser().getUserInfo(future)
        try {
            future.get()
        } catch (e: ExecutionException) {
            throw e.cause!!
        }
    }

    @Test(expected = AuthenticationException::class)
    @Throws(
        Throwable::class)
    fun testUserInfoFailedDueToTokenRemoved() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
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
        val recordedRequest = server.takeRequest()
        val state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .setBody("{}"))
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK)
        enqueue("/userinfo_failed.json", HttpURLConnection.HTTP_UNAUTHORIZED)


        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        val temp = FRUser.getCurrentUser()
        temp.logout()
        Thread.sleep(10) //Make sure trigger the logout first to dequeue the correct message from server
        val future = FRListenerFuture<UserInfo>()
        temp.getUserInfo(future)
        try {
            future.get()
        } catch (e: ExecutionException) {
            throw e.cause!!
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testWithSSO() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage

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
        val recordedRequest = server.takeRequest()
        val state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        enqueue("/userinfo_failed.json", HttpURLConnection.HTTP_UNAUTHORIZED)

        Assert.assertNotNull(nodeListenerFuture.get())

        //AppB

        //setFinalStatic(FRUser.class.getDeclaredField("current"), null);
        EventDispatcher.TOKEN_REMOVED.notifyObservers()
        Config.getInstance().tokenManager.clear()
        Assert.assertNotNull(FRUser.getCurrentUser())
    }

    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        IOException::class,
        JSONException::class,
        ParseException::class,
        AuthenticationRequiredException::class,
        ApiException::class)
    fun testLogout() {
        frUserHappyPath()
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .setBody("{}"))
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK)

        var rr =
            server.takeRequest() //Start the Auth Service POST /json/realms/root/authenticate?authIndexType=service&authIndexValue=Test HTTP/1.1
        Assertions.assertThat(rr.path).isEqualTo("/oauth2/realms/root/access_token")
        rr =
            server.takeRequest() //Post to /user-info endpoint GET /oauth2/realms/root/userinfo HTTP/1.1
        Assertions.assertThat(rr.path).isEqualTo("/oauth2/realms/root/userinfo")

        val accessToken = FRUser.getCurrentUser().accessToken
        Assert.assertNotNull(FRUser.getCurrentUser())
        FRUser.getCurrentUser().logout()
        Assert.assertNull(FRUser.getCurrentUser())

        Assert.assertFalse(Config.getInstance().sessionManager.hasSession())

        val revoke1 = server.takeRequest() //Post to oauth2/realms/root/token/revoke
        val revoke2 = server.takeRequest() //Post to /sessions?_action=logout endpoint

        //revoke Refresh Token and SSO Token are performed async
        val ssoTokenRevoke =
            findRequest("/json/realms/root/sessions?_action=logout", revoke1, revoke2)
        val refreshTokenRevoke = findRequest("/oauth2/realms/root/token/revoke", revoke1, revoke2)

        Assert.assertNotNull(ssoTokenRevoke.getHeader(serverConfig.cookieName))
        Assert.assertEquals(ServerConfig.API_VERSION_3_1,
            ssoTokenRevoke.getHeader(ServerConfig.ACCEPT_API_VERSION))

        val body = refreshTokenRevoke.body.readUtf8()
        Assert.assertTrue(body.contains(OAuth2.TOKEN))
        Assert.assertTrue(body.contains(OAuth2.CLIENT_ID))
        Assert.assertTrue(body.contains(accessToken.refreshToken!!))
        Mockito.verify(mockBroadcastModel)?.sendLogoutBroadcast()
    }

    private fun findRequest(path: String,
                            vararg recordedRequests: RecordedRequest): RecordedRequest {
        for (r in recordedRequests) {
            if (r.path!!.startsWith(path)) {
                return r
            }
        }
        throw IllegalArgumentException()
    }

    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        MalformedURLException::class,
        JSONException::class,
        ParseException::class)
    fun testLogoutFailed() {
        frUserHappyPath()
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .addHeader("Content-Type", "application/json")
            .setBody("""{
    "error_description": "Client authentication failed",
    "error": "invalid_client"
}"""))
        enqueue("/sessions_logout_failed.json", HttpURLConnection.HTTP_OK)
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NO_CONTENT))

        var rr = server.takeRequest() //Post to /access-token endpoint
        rr = server.takeRequest() //Post to /user-info endpoint

        Assert.assertNotNull(FRUser.getCurrentUser())
        FRUser.getCurrentUser().logout()
        Assert.assertNull(FRUser.getCurrentUser())

        Assert.assertFalse(Config.getInstance().sessionManager.hasSession())
        val revoke1 = server.takeRequest() //Post to oauth2/realms/root/token/revoke
        val revoke2 = server.takeRequest() //Post to /sessions?_action=logout endpoint

        rr = findRequest("/json/realms/root/sessions?_action=logout", revoke1, revoke2)
        Assert.assertNotNull(rr.getHeader(serverConfig.cookieName))
        Assert.assertEquals(ServerConfig.API_VERSION_3_1,
            rr.getHeader(ServerConfig.ACCEPT_API_VERSION))
    }

    @Test
    @Throws(ExecutionException::class,
        InterruptedException::class,
        AuthenticationRequiredException::class,
        IOException::class,
        ApiException::class)
    fun testRevokeWithAccessToken() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)



        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
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
        val recordedRequest = server.takeRequest()
        val state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))

        enqueue("/authTreeMockTest_Authenticate_accessToken_no_RefreshToken.json",
            HttpURLConnection.HTTP_OK)
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .setBody("{}"))


        Assert.assertNotNull(nodeListenerFuture.get())

        val accessToken = FRUser.getCurrentUser().accessToken

        //Mock the SSO Token is not stored
        Config.getInstance().singleSignOnManager.clear()

        FRUser.getCurrentUser().logout()
        var rr = server.takeRequest() ///Post to /access-token endpoint
        rr = server.takeRequest() //Post to /oauth2/realms/root/token/revoke

        val body = rr.body.readUtf8()
        Assert.assertTrue(body.contains(OAuth2.TOKEN))
        Assert.assertTrue(body.contains(OAuth2.CLIENT_ID))
        //Using the Access Token to revoke
        Assert.assertTrue(body.contains(accessToken.value))
    }

    @Test
    @Throws(ExecutionException::class,
        InterruptedException::class,
        AuthenticationRequiredException::class,
        IOException::class,
        ApiException::class)
    fun testAccessTokenAndSSOTokenRefreshWithSSOToken() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
        Config.getInstance().url = url

        val nodeListenerFuture: NodeListenerFuture<FRSession> = object : NodeListenerFuture<FRSession>() {
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
        FRSession.authenticate(context, "Example", nodeListenerFuture)

        Assert.assertTrue(nodeListenerFuture.get() is FRSession)

        val tokenManager: TokenManager = DefaultTokenManager.builder()
            .storage(tokenStorage)
            .context(context)
            .build()

        //Check AccessToken Storage
        var future = FRListenerFuture<AccessToken>()
        FRUser.getCurrentUser().getAccessToken(future)
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()

        var recordedRequest = server.takeRequest()
        var state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)

        val accessToken = future.get()
        Assert.assertNotNull(accessToken)
        Assert.assertNotNull(accessToken.value)

        //Check SSOToken Storage
        val singleSignOnManager = Config.getInstance().singleSignOnManager
        val token = singleSignOnManager.token
        Assert.assertNotNull(token)
        Assert.assertNotNull(token.value)

        //Clear the Access Token
        tokenManager.clear()


        future = FRListenerFuture()
        FRUser.getCurrentUser().getAccessToken(future)
        server.takeRequest() // /token endpoint
        recordedRequest = server.takeRequest()
        state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)

        Assert.assertNotNull(future.get().value)
    }

    @Test
    @Throws(ExecutionException::class,
        InterruptedException::class,
        AuthenticationRequiredException::class,
        IOException::class,
        ApiException::class)
    fun testSSOEnabled() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        val storage: Storage<AccessToken> = Memory()

        Config.getInstance().oidcStorage = storage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
        Config.getInstance().url = url

        val nodeListenerFuture: NodeListenerFuture<FRUser> = object : NodeListenerFuture<FRUser>() {
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

        var rr = server.takeRequest() //Start the Auth Service
        rr = server.takeRequest() //Post Name Callback
        rr = server.takeRequest() //Post Password Callback
        rr = server.takeRequest() //Post to /authorize endpoint
        var state = Uri.parse(rr.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        rr = server.takeRequest() //Post to /access-token endpoint

        Assert.assertTrue(nodeListenerFuture.get() is FRUser)

        //Switch to another App with Access Token does not exists
        //So the SSO Token should be used to get the Access Token
        storage.delete()

        val future = FRListenerFuture<AccessToken>()
        FRUser.getCurrentUser().getAccessToken(future)
        rr = server.takeRequest() //Post to /access-token endpoint
        state = Uri.parse(rr.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        Assert.assertNotNull(future.get())
    }

    @Test(expected = AlreadyAuthenticatedException::class)
    @Throws(Throwable::class)
    fun testRelogin() {
        frUserHappyPath()
        val listener: NodeListenerFuture<FRUser> = object : NodeListenerFuture<FRUser>() {
            override fun onCallbackReceived(node: Node) {
            }
        }
        FRUser.login(context, listener)
        try {
            listener.get()
        } catch (e: ExecutionException) {
            throw e.cause!!
        }
    }

    @Test(expected = AlreadyAuthenticatedException::class)
    @Throws(Throwable::class)
    fun testReregister() {
        frUserHappyPath()

        val listener: NodeListenerFuture<FRUser> = object : NodeListenerFuture<FRUser>() {
            override fun onCallbackReceived(node: Node) {
            }
        }
        FRUser.register(context, listener)
        try {
            listener.get()
        } catch (e: ExecutionException) {
            throw e.cause!!
        }
    }

    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        MalformedURLException::class,
        JSONException::class,
        ParseException::class)
    fun testSessionTokenMismatch() {
        frUserHappyPath()

        //We have Access Token now.
        Assert.assertTrue(Config.getInstance().tokenManager.hasToken())


        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success2.json", HttpURLConnection.HTTP_OK)
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .setBody("{}"))

        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
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

        FRSession.authenticate(context, "any", nodeListenerFuture)
        val session = nodeListenerFuture.get()
        Assert.assertEquals("dummy sso token", session.sessionToken.value)

        //Access Token should be removed
        Assert.assertFalse(Config.getInstance().tokenManager.hasToken())
    }

    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        MalformedURLException::class,
        JSONException::class,
        ParseException::class)
    fun testSessionTokenMatch() {
        frUserHappyPath()

        //We have Access Token now.
        Assert.assertTrue(Config.getInstance().tokenManager.hasToken())

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        //Return Same SSO Token
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        val nodeListenerFuture: NodeListenerFuture<FRSession> =
            object : NodeListenerFuture<FRSession>() {
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

        FRSession.authenticate(context, "any", nodeListenerFuture)
        nodeListenerFuture.get()

        //Access Token should not be removed
        Assert.assertTrue(Config.getInstance().tokenManager.hasToken())
    }

    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        IOException::class,
        JSONException::class,
        ParseException::class,
        AuthenticationRequiredException::class,
        ApiException::class)
    fun testSessionTokenUpdated() {
        frUserHappyPath()

        val singleSignOnManager = Config.getInstance().singleSignOnManager
        val tokenManager = Config.getInstance().tokenManager
        var future = FRListenerFuture<AccessToken>()
        tokenManager.getAccessToken(null, future)


        //Make sure the access token is bounded to the Session Token
        Assert.assertEquals(singleSignOnManager.token, future.get().sessionToken)

        //Change the SSO Token, it can be done by SSO scenario.
        singleSignOnManager.persist(SSOToken("New Dummy SSO Token", "", ""))

        future = FRListenerFuture()
        FRUser.getCurrentUser().getAccessToken(future)

        //Since the sso token changed, revoke Refresh Token
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .setBody("{}"))


        //Using new Session Token to get AccessToken
        server.takeRequest() //token
        server.takeRequest() //userinfo
        server.takeRequest() //revoke
        val rr = server.takeRequest() //Post to /authorize endpoint
        val state = Uri.parse(rr.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)

        Assert.assertEquals(singleSignOnManager.token, future.get().sessionToken)
    }

    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        AuthenticationRequiredException::class)
    fun testCustomEndpointAndCookieName() {
        Mockito.`when`(mockContext.getString(R.string.forgerock_oauth_client_id))
            .thenReturn(context.getString(R.string.forgerock_oauth_client_id))
        Mockito.`when`(mockContext.getString(R.string.forgerock_oauth_redirect_uri))
            .thenReturn(context.getString(R.string.forgerock_oauth_redirect_uri))
        Mockito.`when`(mockContext.getString(R.string.forgerock_oauth_scope))
            .thenReturn(context.getString(R.string.forgerock_oauth_scope))
        Mockito.`when`(mockContext.getString(R.string.forgerock_oauth_url))
            .thenReturn(context.getString(R.string.forgerock_oauth_url))
        Mockito.`when`(mockContext.getString(R.string.forgerock_realm))
            .thenReturn(context.getString(R.string.forgerock_realm))
        val resources = Mockito.mock(
            Resources::class.java)
        Mockito.`when`(mockContext.resources).thenReturn(resources)
        Mockito.`when`(mockContext.getString(R.string.forgerock_url))
            .thenReturn("https://dummy.com")
        Mockito.`when`(mockContext.getString(R.string.forgerock_realm)).thenReturn("root")
        Mockito.`when`(mockContext.getString(R.string.forgerock_registration_service))
            .thenReturn("registration")
        Mockito.`when`(mockContext.applicationContext).thenReturn(context)
        Mockito.`when`(resources.getInteger(R.integer.forgerock_timeout)).thenReturn(30)
        Mockito.`when`(resources.getStringArray(R.array.forgerock_ssl_pinning_public_key_hashes))
            .thenReturn(context.resources.getStringArray(R.array.forgerock_ssl_pinning_public_key_hashes))
        Mockito.`when`(mockContext.getString(R.string.forgerock_authenticate_endpoint))
            .thenReturn("dummy/authenticate")
        Mockito.`when`(mockContext.getString(R.string.forgerock_authorize_endpoint))
            .thenReturn("dummy/authorize")
        Mockito.`when`(mockContext.getString(R.string.forgerock_token_endpoint))
            .thenReturn("dummy/token")
        Mockito.`when`(mockContext.getString(R.string.forgerock_userinfo_endpoint))
            .thenReturn("dummy/userinfo")
        Mockito.`when`(mockContext.getString(R.string.forgerock_revoke_endpoint))
            .thenReturn("dummy/revoke")
        Mockito.`when`(mockContext.getString(R.string.forgerock_session_endpoint))
            .thenReturn("dummy/logout")
        Mockito.`when`(mockContext.getString(R.string.forgerock_endsession_endpoint))
            .thenReturn("dummy/endSession")
        Mockito.`when`(mockContext.getString(R.string.forgerock_cookie_name))
            .thenReturn("testCookieName")
        Mockito.`when`(mockContext.getString(R.string.forgerock_auth_service))
            .thenReturn("UsernamePassword")
        Mockito.`when`(mockContext.getString(R.string.forgerock_oauth_sign_out_redirect_uri))
            .thenReturn(context.getString(R.string.forgerock_oauth_sign_out_redirect_uri))

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.reset()
        Config.getInstance().init(mockContext, null)
        serverConfig = Config.getInstance().serverConfig

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
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

        FRUser.login(mockContext, nodeListenerFuture)

        var rr =
            server.takeRequest() //Start the Auth Service POST /json/realms/root/authenticate?authIndexType=service&authIndexValue=UsernamePassword HTTP/1.1
        Assertions.assertThat(rr.path)
            .isEqualTo("/dummy/authenticate?authIndexType=service&authIndexValue=UsernamePassword")
        rr = server.takeRequest() //Post Name Callback POST /json/realms/root/authenticate HTTP/1.1
        Assertions.assertThat(rr.path).isEqualTo("/dummy/authenticate")
        rr =
            server.takeRequest() //Post Password Callback POST /json/realms/root/authenticate HTTP/1.1
        Assertions.assertThat(rr.path).isEqualTo("/dummy/authenticate")
        rr =
            server.takeRequest() //Post to /authorize endpoint GET /oauth2/realms/root/authorize?iPlanetDirectoryPro=C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*&client_id=andy_app&scope=openid&response_type=code&redirect_uri=https%3A%2F%2Fwww.example.com%3A8080%2Fcallback&code_challenge=PnQUh9V3GPr5qamcKZ39fcv4o81KJbhYls89L5rkVs8&code_challenge_method=S256 HTTP/1.1
        Assertions.assertThat(rr.path).startsWith("/dummy/authorize")

        val state = Uri.parse(rr.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK)

        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        val future = FRListenerFuture<UserInfo>()
        FRUser.getCurrentUser().getUserInfo(future)
        future.get()

        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .setBody("{}"))
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK)
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_NO_CONTENT))

        rr =
            server.takeRequest() //Post to /access-token endpoint POST /oauth2/realms/root/access_token HTTP/1.1
        Assertions.assertThat(rr.path).isEqualTo("/dummy/token")
        rr =
            server.takeRequest() //Post to /user-info endpoint GET /oauth2/realms/root/userinfo HTTP/1.1
        Assertions.assertThat(rr.path).isEqualTo("/dummy/userinfo")

        val accessToken = FRUser.getCurrentUser().accessToken
        Assert.assertNotNull(FRUser.getCurrentUser())

        FRUser.getCurrentUser().logout()
        Assert.assertNull(FRUser.getCurrentUser())

        Assert.assertFalse(Config.getInstance().sessionManager.hasSession())

        val revoke1 = server.takeRequest() //Post to oauth2/realms/root/token/revoke
        val revoke2 = server.takeRequest() //Post to /sessions?_action=logout endpoint

        val ssoTokenRevoke = findRequest("/dummy/logout?_action=logout", revoke1, revoke2)
        val refreshTokenRevoke = findRequest("/dummy/revoke", revoke1, revoke2)

        Assertions.assertThat(ssoTokenRevoke.getHeader("testCookieName")).isNotNull()
        Assert.assertEquals(ServerConfig.API_VERSION_3_1,
            ssoTokenRevoke.getHeader(ServerConfig.ACCEPT_API_VERSION))

        val body = refreshTokenRevoke.body.readUtf8()
        Assert.assertTrue(body.contains(OAuth2.TOKEN))
        Assert.assertTrue(body.contains(OAuth2.CLIENT_ID))
        Assert.assertTrue(body.contains(accessToken.refreshToken!!))
    }

    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        MalformedURLException::class,
        JSONException::class,
        ParseException::class)
    fun testRequestInterceptor() {
        //Total e request will be intercepted

        val countDownLatch = CountDownLatch(8)
        val result = HashMap<String, Pair<Action, Int>>()
        RequestInterceptorRegistry.getInstance().register(RequestInterceptor { request: Request ->
            countDownLatch.countDown()
            val action = (request.tag() as Action).type
            val pair = result[action]
            if (pair == null) {
                result[action] = Pair(request.tag() as Action, 1)
            } else {
                result[action] = Pair(request.tag() as Action, pair.second + 1)
            }
            request
        })

        frUserHappyPath()

        //Logout
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .setBody("{}"))
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK)


        FRUser.getCurrentUser().logout()
        countDownLatch.await()

        Assertions.assertThat(result["START_AUTHENTICATE"]!!.first.payload.getString("tree"))
            .isEqualTo("Test")
        Assertions.assertThat(result["START_AUTHENTICATE"]!!.first.payload.getString("type"))
            .isEqualTo("service")
        Assertions.assertThat(result["START_AUTHENTICATE"]!!.second).isEqualTo(1)
        Assertions.assertThat(result["AUTHENTICATE"]!!.first.payload.getString("tree"))
            .isEqualTo("Test")
        Assertions.assertThat(result["AUTHENTICATE"]!!.first.payload.getString("type"))
            .isEqualTo("service")
        Assertions.assertThat(result["AUTHENTICATE"]!!.second).isEqualTo(2)
        Assertions.assertThat(result["AUTHORIZE"]!!.second).isEqualTo(1)
        Assertions.assertThat(result["EXCHANGE_TOKEN"]!!.second).isEqualTo(1)
        Assertions.assertThat(result["REVOKE_TOKEN"]!!.second).isEqualTo(1)
        Assertions.assertThat(result["LOGOUT"]!!.second).isEqualTo(1)
        //Assertions.assertThat(result.get("END_SESSION").second).isEqualTo(1);
        Assertions.assertThat(result["USER_INFO"]!!.second).isEqualTo(1)
    }

    @Test
    @Throws(InterruptedException::class,
        ExecutionException::class,
        MalformedURLException::class,
        JSONException::class,
        ParseException::class)
    fun testCookieInterceptor() {
        //Total e request will be intercepted

        val countDownLatch = CountDownLatch(8)

        RequestInterceptorRegistry.getInstance().register(object: CustomCookieInterceptor() {
            override fun intercept(cookies: List<Cookie>): List<Cookie> {
                countDownLatch.countDown()
                val customizedCookies: MutableList<Cookie> = ArrayList()
                customizedCookies.add(Cookie.Builder().domain("localhost").name("test").value("testValue")
                    .build())
                customizedCookies.addAll(cookies)
                return customizedCookies
            }
        })

        frUserHappyPath()

        //Logout
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .setBody("{}"))
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK)


        FRUser.getCurrentUser().logout()
        countDownLatch.await()

        //Take few requests and make sure it contains the custom header.
        var recordedRequest = server.takeRequest()
        Assertions.assertThat(recordedRequest.getHeader("Cookie")).isEqualTo("test=testValue")
        recordedRequest = server.takeRequest()
        Assertions.assertThat(recordedRequest.getHeader("Cookie")).isEqualTo("test=testValue")
        recordedRequest = server.takeRequest()
    }


    @Test
    @Throws(ExecutionException::class,
        InterruptedException::class,
        AuthenticationRequiredException::class,
        ApiException::class,
        IOException::class)
    fun testAccessTokenWithExpiredRefreshToken() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
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
        var rr = server.takeRequest()
        var state = Uri.parse(rr.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken_shortlife.json",
            HttpURLConnection.HTTP_OK)

        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        //Refresh failed due to invalid grant (Assumption that invalid grant = refresh token expired)
        server.enqueue(MockResponse()
            .setBody("""{
    "error_description": "grant is invalid",
    "error": "invalid_grant"
}""")
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST))
        //Let the token expire, so that we use the refresh token flow
        Thread.sleep(1000)

        server.takeRequest() //access_token

        //Use the session token to retrieve the Access token
        //Assert that we retrieve the new token
        val future = FRListenerFuture<AccessToken>()
        FRUser.getCurrentUser().getAccessToken(future)

        server.takeRequest() //revoke
        rr = server.takeRequest()
        state = Uri.parse(rr.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)

        Assertions.assertThat(future.get().expiresIn).isEqualTo(3599)
        Assertions.assertThat(FRSession.getCurrentSession()).isNotNull()
    }

    @Test
    @Throws(ExecutionException::class,
        InterruptedException::class,
        ApiException::class,
        IOException::class)
    fun testAccessTokenWithExpiredRefreshTokenFailedToRefreshWithSSOToken() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
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
        val rr = server.takeRequest()
        val state = Uri.parse(rr.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken_shortlife.json",
            HttpURLConnection.HTTP_OK)

        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        //Refresh failed due to invalid grant (Assumption that invalid grant = refresh token expired)
        server.enqueue(MockResponse()
            .setBody("""{
    "error_description": "grant is invalid",
    "error": "invalid_grant"
}""")
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST))
        //Let the token expire, so that we use the refresh token flow
        Thread.sleep(1000)

        //Use the session token to retrieve the Access token
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST))

        try {
            FRUser.getCurrentUser().accessToken
            Assert.fail()
        } catch (e: AuthenticationRequiredException) {
        }

        Assertions.assertThat(FRUser.getCurrentUser()).isNull()
        Assertions.assertThat(FRSession.getCurrentSession()).isNull()
    }

    @Ignore("For 3.0")
    @Test
    @Throws(ExecutionException::class,
        InterruptedException::class,
        ApiException::class,
        IOException::class,
        AuthenticationRequiredException::class)
    fun testAccessTokenWithInvalidClientDuringRefresh() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
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
        val rr = server.takeRequest()
        val state = Uri.parse(rr.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken_shortlife.json",
            HttpURLConnection.HTTP_OK)

        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        val error = """{
    "error_description": "client is invalid",
    "error": "invalid_client"
}"""
        //Refresh failed due to invalid client
        server.enqueue(MockResponse()
            .setBody(error)
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST))
        //Let the token expire, so that we use the refresh token flow
        Thread.sleep(1000)

        try {
            FRUser.getCurrentUser().accessToken
            Assert.fail()
        } catch (e: Exception) {
            val exception = e as ApiException
            Assertions.assertThat(exception.statusCode)
                .isEqualTo(HttpURLConnection.HTTP_BAD_REQUEST)
            Assertions.assertThat(exception.message).isEqualTo(error)
        }

        Assertions.assertThat(FRUser.getCurrentUser()).isNotNull()
        Assertions.assertThat(FRSession.getCurrentSession()).isNotNull()
    }

    @Test
    @Throws(ExecutionException::class,
        InterruptedException::class,
        ApiException::class,
        IOException::class,
        AuthenticationRequiredException::class)
    fun testAccessTokenWithoutRefresh() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
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
        val rr = server.takeRequest()
        val state = Uri.parse(rr.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken_no_RefreshToken_shortlife.json",
            HttpURLConnection.HTTP_OK)

        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())

        //Let the token expire, so that we use the refresh token flow
        Thread.sleep(1000)

        //Use the session token to retrieve the Access token
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST))

        try {
            FRUser.getCurrentUser().accessToken
            Assert.fail()
        } catch (e: AuthenticationRequiredException) {
        }

        Assertions.assertThat(FRUser.getCurrentUser()).isNull()
        Assertions.assertThat(FRSession.getCurrentSession()).isNull()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testAccessTokenWithUnmatchSSOToken() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
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
        val rr = server.takeRequest()
        val state = Uri.parse(rr.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))

        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        //revoke Access Token
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .addHeader("Content-Type", "application/json")
            .setBody("{}"))

        //endsession
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK)


        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())
        //SSO Token has been updated, user may sing in with another user with a different SSOToken
        Config.getInstance().singleSignOnManager.persist(SSOToken("UpdatedSSOToken", "", ""))

        try {
            FRUser.getCurrentUser().accessToken
            Assert.fail()
        } catch (e: AuthenticationRequiredException) {
            //expect exception
        }

        Assertions.assertThat(FRUser.getCurrentUser()).isNull()
        Assertions.assertThat(FRSession.getCurrentSession()).isNull()
    }

    @Test
    @Throws(ExecutionException::class,
        InterruptedException::class,
        AuthenticationRequiredException::class)
    fun testAccessTokenRestoreSSOToken() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().oidcStorage = tokenStorage
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage
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

        val rr = server.takeRequest()
        val state = Uri.parse(rr.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK)


        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertNotNull(FRUser.getCurrentUser())
        //Remove the SSOToken
        Config.getInstance().singleSignOnManager.revoke(null)

        Assertions.assertThat(FRUser.getCurrentUser().accessToken).isNotNull()
        Assertions.assertThat(Config.getInstance().singleSignOnManager.token).isNotNull()
    }

    private abstract class CustomCookieInterceptor : FRRequestInterceptor<Action>, CookieInterceptor {
        override fun intercept(request: Request, tag: Action): Request {
            return request
        }
    }

    companion object {
        private const val DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest"
        private const val DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest"

        @Throws(Exception::class)
        fun setFinalStatic(field: Field, newValue: Any?) {
            field.isAccessible = true
            field[null] = newValue
        }
    }
}
