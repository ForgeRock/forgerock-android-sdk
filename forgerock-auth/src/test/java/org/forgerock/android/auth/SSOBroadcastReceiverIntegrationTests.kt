/*
 *
 *  * Copyright (c) 2022 ForgeRock. All rights reserved.
 *  *
 *  * This software may be modified and distributed under the terms
 *  * of the MIT license. See the LICENSE file for details.
 *
 *
 */

package org.forgerock.android.auth

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.*
import org.forgerock.android.auth.callback.NameCallback
import org.forgerock.android.auth.callback.PasswordCallback
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@RunWith(AndroidJUnit4::class)
class SSOBroadcastReceiverIntegrationTests: BaseTest() {

    private val defaultTokenManagerTest = "DefaultTokenManagerTest"
    private val defaultSSOTokenManagerTest = "DefaultSSOManagerTest"

    private fun findRequest(path: String, vararg recordedRequests: RecordedRequest): RecordedRequest {
        for (r: RecordedRequest in recordedRequests) {
            if (r.path.startsWith(path)) {
                return r
            }
        }
        throw IllegalArgumentException()
    }

    private fun frUserHappyPath() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)
        server.enqueue(
            MockResponse()
                .addHeader(
                    "Location",
                    "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app"
                )
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
        )
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK)
        Config.getInstance().sharedPreferences =
            context.getSharedPreferences(
                defaultTokenManagerTest,
                Context.MODE_PRIVATE
            )
        Config.getInstance().ssoSharedPreferences =
            context.getSharedPreferences(
                defaultSSOTokenManagerTest,
                Context.MODE_PRIVATE
            )
        Config.getInstance().url = url
        Config.getInstance().encryptor = MockEncryptor()
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
        assertNotNull(nodeListenerFuture.get())
        assertNotNull(FRUser.getCurrentUser())
        val future = FRListenerFuture<UserInfo>()
        FRUser.getCurrentUser().getUserInfo(future)
        val userinfo = future.get()
        assertEquals("sub", userinfo.sub)
        assertEquals("name", userinfo.name)
        assertEquals("given name", userinfo.givenName)
        assertEquals("family name", userinfo.familyName)
        assertEquals("middle name", userinfo.middleName)
        assertEquals("nick name", userinfo.nickName)
        assertEquals("preferred username", userinfo.preferredUsername)
        assertEquals(URL("http://profile"), userinfo.profile)
        assertEquals(URL("http://picture"), userinfo.picture)
        assertEquals(URL("http://website"), userinfo.website)
        assertEquals("test@email.com", userinfo.email)
        assertEquals(true, userinfo.emailVerified)
        assertEquals("male", userinfo.gender)
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        assertEquals(simpleDateFormat.parse("2008-01-30"), userinfo.birthDate)
        assertEquals("zoneinfo", userinfo.zoneInfo)
        assertEquals("locale", userinfo.locale)
        assertEquals("phone number", userinfo.phoneNumber)
        assertEquals(true, userinfo.phoneNumberVerified)
        assertEquals("800000", userinfo.updateAt.toString())
        assertEquals("formatted", userinfo.address.formatted)
        assertEquals("street address", userinfo.address.streetAddress)
        assertEquals("locality", userinfo.address.locality)
        assertEquals("region", userinfo.address.region)
        assertEquals("90210", userinfo.address.postalCode)
        assertEquals("US", userinfo.address.country)
        assertEquals(getJson("/userinfo_success.json"), userinfo.raw.toString(2))
    }

    @Test
    fun logoutWhenBroadcastLogOutEvent() {
        frUserHappyPath()
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}")
        )
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK)
        var rr: RecordedRequest =
            server.takeRequest() //Start the Auth Service POST /json/realms/root/authenticate?authIndexType=service&authIndexValue=Test HTTP/1.1
        assertThat(rr.path)
            .isEqualTo("/json/realms/root/authenticate?authIndexType=service&authIndexValue=Test")
        rr = server.takeRequest() //Post Name Callback POST /json/realms/root/authenticate HTTP/1.1
        assertThat(rr.path).isEqualTo("/json/realms/root/authenticate")
        rr =
            server.takeRequest() //Post Password Callback POST /json/realms/root/authenticate HTTP/1.1
        assertThat(rr.path).isEqualTo("/json/realms/root/authenticate")
        rr =
            server.takeRequest() //Post to /authorize endpoint GET /oauth2/realms/root/authorize?iPlanetDirectoryPro=C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*&client_id=andy_app&scope=openid&response_type=code&redirect_uri=https%3A%2F%2Fwww.example.com%3A8080%2Fcallback&code_challenge=PnQUh9V3GPr5qamcKZ39fcv4o81KJbhYls89L5rkVs8&code_challenge_method=S256 HTTP/1.1
        assertThat(rr.path).startsWith("/oauth2/realms/root/authorize")
        rr =
            server.takeRequest() //Post to /access-token endpoint POST /oauth2/realms/root/access_token HTTP/1.1
        assertThat(rr.path).isEqualTo("/oauth2/realms/root/access_token")
        rr =
            server.takeRequest() //Post to /user-info endpoint GET /oauth2/realms/root/userinfo HTTP/1.1
        assertThat(rr.path).isEqualTo("/oauth2/realms/root/userinfo")
        val accessToken = FRUser.getCurrentUser().accessToken
        assertNotNull(FRUser.getCurrentUser())

        val testObject = SSOBroadcastReceiver()
        val intent = Intent("org.forgerock.android.auth.broadcast.SSO_LOGOUT")
        intent.putExtra("BROADCAST_PACKAGE_KEY", "com.test.forgerock")
        testObject.onReceive(context, intent)

        assertNull(FRUser.getCurrentUser())
        val sessionManagerObject: SessionManager = Config.getInstance().sessionManager

        sessionManagerObject.hasSession()

        val revoke1: RecordedRequest =
            server.takeRequest() //Post to oauth2/realms/root/token/revoke
        val revoke2: RecordedRequest =
            server.takeRequest() //Post to /sessions?_action=logout endpoint

        //revoke Refresh Token and SSO Token are performed async
        val ssoTokenRevoke: RecordedRequest =
            findRequest("/json/realms/root/sessions?_action=logout", revoke1, revoke2)
        val refreshTokenRevoke: RecordedRequest =
            findRequest("/oauth2/realms/root/token/revoke", revoke1, revoke2)
        assertNotNull(ssoTokenRevoke.getHeader(serverConfig.cookieName))
        assertEquals(
            ServerConfig.API_VERSION_3_1,
            ssoTokenRevoke.getHeader(ServerConfig.ACCEPT_API_VERSION)
        )
        val body = refreshTokenRevoke.body.readUtf8()
        assertTrue(body.contains("token"))
        assertTrue(body.contains("client_id"))
        assertTrue(body.contains(accessToken.refreshToken))
    }
}

