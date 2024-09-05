/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.net.Uri
import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.exception.ApiException
import org.forgerock.android.auth.exception.AuthorizeException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.HttpURLConnection
import java.util.concurrent.ExecutionException

@RunWith(RobolectricTestRunner::class)
class OAuth2MockTest : BaseTest() {
    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun oAuth2Success() {
        val token = SSOToken("ssoToken")
        val oAuth2TokenListenerFuture = OAuth2TokenListenerFuture()
        oAuth2Client.exchangeToken(token, emptyMap(), oAuth2TokenListenerFuture)
        val recordedRequest = server.takeRequest() //authorize
        Assertions.assertThat(recordedRequest.getHeader(serverConfig.cookieName))
            .isEqualTo(token.value)
        val state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)

        Assert.assertNotNull(oAuth2TokenListenerFuture.get())
        val accessToken = oAuth2TokenListenerFuture.get()
        Assert.assertNotNull(accessToken.value)
        Assert.assertNotNull(accessToken.refreshToken)
        Assert.assertNotNull(accessToken.idToken)
        Assert.assertEquals(3, accessToken.scope!!.size.toLong())
        Assert.assertTrue(accessToken.scope!!.contains("openid"))
        Assert.assertTrue(accessToken.scope!!.contains("email"))
        Assert.assertTrue(accessToken.scope!!.contains("address"))
        Assert.assertEquals("Bearer", accessToken.tokenType)
        Assert.assertEquals(3599, accessToken.expiresIn)

        /*
        RecordedRequest recordedRequest = server.takeRequest(); //authorize
         */
    }

    @Test
    fun oAuth2FailedOnInvalidRedirect() {
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST))

        val token = SSOToken("ssoToken")
        val oAuth2TokenListenerFuture = OAuth2TokenListenerFuture()
        oAuth2Client.exchangeToken(token, emptyMap(), oAuth2TokenListenerFuture)

        try {
            Assert.assertNotNull(oAuth2TokenListenerFuture.get())
            Assert.fail()
        } catch (e: ExecutionException) {
            val authorizeException = e.cause as AuthorizeException?
            Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST.toLong(),
                (authorizeException!!.cause as ApiException?)!!.statusCode.toLong())
        } catch (e: InterruptedException) {
            Assert.fail()
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun oAuth2FailedOnInvalidSession() {
        val token = SSOToken("ssoToken")
        val oAuth2TokenListenerFuture = OAuth2TokenListenerFuture()
        oAuth2Client.exchangeToken(token, emptyMap(), oAuth2TokenListenerFuture)

        val recordedRequest = server.takeRequest() //authorize
        val state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?error_description=Failed%20to%20get%20resource%20owner%20session%20from%20request&" +
                        "state=" + state + "&error=invalid_request")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))

        try {
            Assert.assertNotNull(oAuth2TokenListenerFuture.get())
            Assert.fail()
        } catch (e: ExecutionException) {
            val authorizeException = e.cause as AuthorizeException?
            Assert.assertEquals(HttpURLConnection.HTTP_MOVED_TEMP.toLong(),
                (authorizeException!!.cause as ApiException?)!!.statusCode.toLong())
            Assert.assertEquals("Failed to get resource owner session from request",
                (authorizeException.cause as ApiException?)!!.message)
        } catch (e: InterruptedException) {
            Assert.fail()
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun oAuth2FailedWithInvalidAuthCode() {
        val errorMessage = """{
    "error_description": "The provided access grant is invalid, expired, or revoked.",
    "error": "invalid_grant"
}"""


        val token = SSOToken("ssoToken")
        val oAuth2TokenListenerFuture = OAuth2TokenListenerFuture()
        oAuth2Client.exchangeToken(token, emptyMap(), oAuth2TokenListenerFuture)

        val recordedRequest = server.takeRequest() //authorize
        val state = Uri.parse(recordedRequest.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        server.enqueue(MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .setBody(errorMessage)
        )



        try {
            oAuth2TokenListenerFuture.get()
            Assert.fail()
        } catch (e: ExecutionException) {
            Assert.assertTrue(e.cause is ApiException)
            Assert.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST.toLong(),
                (e.cause as ApiException?)!!.statusCode.toLong())
            Assert.assertEquals(errorMessage, e.cause!!.message)
        } catch (e: InterruptedException) {
            Assert.fail()
        }
    }

    @Test
    fun oAuth2InvalidState() {
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)

        val token = SSOToken("ssoToken")
        val oAuth2TokenListenerFuture = OAuth2TokenListenerFuture()
        oAuth2Client.exchangeToken(token, emptyMap(), oAuth2TokenListenerFuture)

        try {
            oAuth2TokenListenerFuture.get()
            Assert.fail()
        } catch (e: ExecutionException) {
            Assert.assertTrue(e.cause is AuthorizeException)
            Assert.assertTrue(e.cause!!.cause is IllegalStateException)
        } catch (e: InterruptedException) {
            Assert.fail()
        }
    }
}
