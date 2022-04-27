/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Pair
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.RecordedRequest
import net.openid.appauth.*
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.exception.AlreadyAuthenticatedException
import org.forgerock.android.auth.exception.ApiException
import org.forgerock.android.auth.exception.AuthenticationRequiredException
import org.forgerock.android.auth.exception.BrowserAuthenticationException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class BrowserLoginTest : BaseTest() {
    private fun getAppAuthFragment(activity: FragmentActivity): AppAuthFragment? {
        val fragment = activity.supportFragmentManager
            .findFragmentByTag(AppAuthFragment.TAG)
        return fragment as? AppAuthFragment
    }

    @Test
    @Throws(
        InterruptedException::class,
        ExecutionException::class,
        AuthenticationRequiredException::class,
        IOException::class,
        ApiException::class
    )
    fun testHappyPath() {
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        Config.getInstance().sharedPreferences = context.getSharedPreferences(
            DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE
        )
        Config.getInstance().ssoSharedPreferences = context.getSharedPreferences(
            DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE
        )
        Config.getInstance().url = url
        Config.getInstance().encryptor = MockEncryptor()

        val scenario: ActivityScenario<DummyActivity> = ActivityScenario.launch(DummyActivity::class.java)
        scenario.onActivity {
            InitProvider.setCurrentActivity(it)
        }
        val future = FRListenerFuture<FRUser>()
        scenario.onActivity {
            FRUser.browser().failedOnNoBrowserFound(false)
                .login(it, future)
        }

        //  AppAuthFragment appAuthFragment = getAppAuthFragment(fragmentActivity);
        val intent = Intent()
        intent.putExtra(
            AuthorizationResponse.EXTRA_RESPONSE,
            "{\"request\":{\"configuration\":{\"authorizationEndpoint\":\"http:\\/\\/openam.example.com:8081\\/openam\\/oauth2\\/realms\\/root\\/authorize\",\"tokenEndpoint\":\"http:\\/\\/openam.example.com:8081\\/openam\\/oauth2\\/realms\\/root\\/access_token\"},\"clientId\":\"AndroidTest\",\"responseType\":\"code\",\"redirectUri\":\"net.openid.appauthdemo2:\\/oauth2redirect\",\"login_hint\":\"login\",\"scope\":\"openid profile email address phone\",\"state\":\"2v0SIhB7UAmsqvnvwR-IKQ\",\"codeVerifier\":\"qvCFoo3tqB-89lYOFjX2ZAMalkKgW_KIcc1tN3hmx3ygOHyYbWT9hKU7rhky6ivB-33exlhyyHHeSJtuVfOobg\",\"codeVerifierChallenge\":\"i-UW4h0UlD_pt1WCYGeP6prmtOkXhyQB_s1itrkV68k\",\"codeVerifierChallengeMethod\":\"S256\",\"additionalParameters\":{}},\"state\":\"2v0SIhB7UAmsqvnvwR-IKQ\",\"code\":\"roxwkG0TtooR2vzA6z0MT9xyJSQ\",\"additional_parameters\":{\"iss\":\"http:\\/\\/openam.example.com:8081\\/openam\\/oauth2\",\"client_id\":\"andy_app\"}}"
        )
        scenario.onActivity {
            getAppAuthFragment(it)?.onActivityResult(
                AppAuthFragment.AUTH_REQUEST_CODE, Activity.RESULT_OK, intent
            )
        }

        // appAuthFragment.onActivityResult(AppAuthFragment.AUTH_REQUEST_CODE, Activity.RESULT_OK, intent);
        val frUser = future.get()
        Assertions.assertThat(frUser.accessToken).isNotNull
        val rr =
            server.takeRequest() //Start the Auth Service POST /json/realms/root/authenticate?authIndexType=service&authIndexValue=Test HTTP/1.1
        Assertions.assertThat(rr.path).isEqualTo("/oauth2/realms/root/access_token")
        Assertions.assertThat(rr.method).isEqualTo("POST")
        val body = parse(rr.body.readUtf8())
        Assertions.assertThat(body["client_id"]).isEqualTo("andy_app")
        Assertions.assertThat(body["code_verifier"])
            .isEqualTo("qvCFoo3tqB-89lYOFjX2ZAMalkKgW_KIcc1tN3hmx3ygOHyYbWT9hKU7rhky6ivB-33exlhyyHHeSJtuVfOobg")
        Assertions.assertThat(body["grant_type"]).isEqualTo("authorization_code")
        Assertions.assertThat(body["code"]).isEqualTo("roxwkG0TtooR2vzA6z0MT9xyJSQ")
    }

    @Test
    @Throws(
        InterruptedException::class,
        ExecutionException::class,
        AuthenticationRequiredException::class,
        IOException::class,
        ApiException::class
    )
    fun testLogout() {
        testHappyPath()
        //Access Token Revoke
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}")
        )
        //ID Token endsession
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_NO_CONTENT)
        )
        FRUser.getCurrentUser().logout()
        Assert.assertNull(FRUser.getCurrentUser())
        Assert.assertFalse(Config.getInstance().sessionManager.hasSession())
        val revoke1 = server.takeRequest() //Post to oauth2/realms/root/token/revoke
        val revoke2 = server.takeRequest() //Post to /endSession

        //revoke Refresh Token and SSO Token are performed async
        val refreshTokenRevoke = findRequest("/oauth2/realms/root/token/revoke", revoke1, revoke2)
        val endSession = findRequest("/oauth2/realms/root/connect/endSession", revoke1, revoke2)
        Assertions.assertThat(refreshTokenRevoke).isNotNull
        Assertions.assertThat(
            Uri.parse(endSession.path).getQueryParameter("id_token_hint")
        ).isNotNull
    }

    @Test
    @Throws(
        InterruptedException::class,
        ExecutionException::class,
        AuthenticationRequiredException::class,
        IOException::class,
        ApiException::class
    )
    fun testRevokeTokenFailed() {
        testHappyPath()
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    """{
    "error_description": "Client authentication failed",
    "error": "invalid_client"
}"""
                )
        )
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_NO_CONTENT))
        FRUser.getCurrentUser().logout()
        Assert.assertNull(FRUser.getCurrentUser())
        Assert.assertFalse(Config.getInstance().sessionManager.hasSession())
        val revoke1 = server.takeRequest() //Post to oauth2/realms/root/token/revoke
        val revoke2 = server.takeRequest() //Post to /endSession

        //revoke Refresh Token and SSO Token are performed async
        val refreshTokenRevoke = findRequest("/oauth2/realms/root/token/revoke", revoke1, revoke2)
        val endSession = findRequest("/oauth2/realms/root/connect/endSession", revoke1, revoke2)
        Assertions.assertThat(refreshTokenRevoke).isNotNull

        //Make sure we still invoke the endSession
        Assertions.assertThat(
            Uri.parse(endSession.path).getQueryParameter("id_token_hint")
        ).isNotNull
    }

    //It is running with JVM, no browser is expected
    @Test(expected = ActivityNotFoundException::class)
    @Throws(
        InterruptedException::class,
        ExecutionException::class,
        AuthenticationRequiredException::class
    )
    fun testAppAuthConfigurer() {
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)

        val invoked = AtomicInteger()
        val browser = FRUser.browser().appAuthConfigurer()
            .authorizationRequest { builder: AuthorizationRequest.Builder ->
                //Test populated data
                val request = builder.build()
                Assertions.assertThat(request.clientId).isEqualTo(oAuth2Client.clientId)
                Assertions.assertThat(request.redirectUri)
                    .isEqualTo(Uri.parse(oAuth2Client.redirectUri))
                Assertions.assertThat(request.scope).isEqualTo(oAuth2Client.scope)
                Assertions.assertThat(request.responseType).isEqualTo(oAuth2Client.responseType)
                invoked.getAndIncrement()
            }
            .appAuthConfiguration { builder: AppAuthConfiguration.Builder ->
                Assertions.assertThat(builder).isNotNull
                invoked.getAndIncrement()
            }
            .customTabsIntent { builder: CustomTabsIntent.Builder ->
                Assertions.assertThat(builder).isNotNull
                invoked.getAndIncrement()
            }
            .authorizationServiceConfiguration {
                invoked.getAndIncrement()
                try {
                    return@authorizationServiceConfiguration AuthorizationServiceConfiguration(
                        Uri.parse(oAuth2Client.authorizeUrl.toString()),
                        Uri.parse(oAuth2Client.tokenUrl.toString())
                    )
                } catch (e: MalformedURLException) {
                    throw RuntimeException(e)
                }
            }.done()

        val scenario = launchFragmentInContainer<AppAuthFragment>(
            initialState = Lifecycle.State.INITIALIZED
        )

        scenario.onFragment {
            it.setBrowser(browser)
        }
        scenario.moveToState(Lifecycle.State.CREATED)
        Assertions.assertThat(invoked.get()).isEqualTo(4)

    }

    @Test
    @Throws(InterruptedException::class)
    fun testOperationCancel() {
        val scenario: ActivityScenario<DummyActivity> = ActivityScenario.launch(DummyActivity::class.java)

        val future = FRListenerFuture<FRUser>()

        scenario.onActivity {
            FRUser.browser().failedOnNoBrowserFound(false)
                .login(it, future)
        }

        val intent = Intent()
        intent.putExtra(
            AuthorizationException.EXTRA_EXCEPTION,
            "{\"type\":0,\"code\":1,\"errorDescription\":\"User cancelled flow\"}"
        )

        scenario.onActivity {
            getAppAuthFragment(it)?.onActivityResult(
                AppAuthFragment.AUTH_REQUEST_CODE, Activity.RESULT_CANCELED, intent
            )
        }

        try {
            future.get()
            Assert.fail()
        } catch (e: ExecutionException) {
            Assertions.assertThat(e.cause).isInstanceOf(BrowserAuthenticationException::class.java)
            Assertions.assertThat(e.cause?.message)
                .isEqualTo("{\"type\":0,\"code\":1,\"errorDescription\":\"User cancelled flow\"}")
        }
    }

    @Test(expected = AlreadyAuthenticatedException::class)
    @Throws(
        Throwable::class
    )
    fun testUserAlreadyAuthenticate() {
        testHappyPath()

        val scenario: ActivityScenario<DummyActivity> = ActivityScenario.launch(DummyActivity::class.java)
        scenario.onActivity {
            val future = FRListenerFuture<FRUser>()
            FRUser.browser().login(it, future)
            try {
                future.get()
            } catch (e: ExecutionException) {
                throw e.cause ?: Throwable("unknown error")
            }
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testInvalidScope() {
        val scenario: ActivityScenario<DummyActivity> = ActivityScenario.launch(DummyActivity::class.java)

        val future = FRListenerFuture<FRUser>()

        scenario.onActivity {
            FRUser.browser().failedOnNoBrowserFound(false)
                .login(it, future)
        }

        val intent = Intent()
        intent.putExtra(AuthorizationException.EXTRA_EXCEPTION, INVALID_SCOPE)
        scenario.onActivity {
            getAppAuthFragment(it)?.onActivityResult(
                AppAuthFragment.AUTH_REQUEST_CODE, Activity.RESULT_OK, intent
            )
        }

        try {
            future.get()
            Assert.fail()
        } catch (e: ExecutionException) {
            Assertions.assertThat(e.cause).isInstanceOf(BrowserAuthenticationException::class.java)
            Assertions.assertThat(e.cause?.message).isEqualTo(INVALID_SCOPE)
        }
    }

    @Test
    @Throws(
        InterruptedException::class,
        ExecutionException::class,
        AuthenticationRequiredException::class,
        IOException::class,
        ApiException::class
    )
    fun testRequestInterceptor() {
        val result = HashMap<String, Pair<Action, Int>>()
        RequestInterceptorRegistry.getInstance().register(RequestInterceptor { request: Request ->
            val action = (request.tag() as Action).type
            val pair = result[action]
            if (pair == null) {
                result[action] = Pair(request.tag() as Action, 1)
            } else {
                result[action] = Pair(request.tag() as Action, pair.second + 1)
            }
            request
        })
        testHappyPath()
        //Access Token Revoke
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}")
        )
        //ID Token endsession
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_NO_CONTENT)
        )
        FRUser.getCurrentUser().logout()
        Assert.assertNull(FRUser.getCurrentUser())
        Assert.assertFalse(Config.getInstance().sessionManager.hasSession())
        val revoke1 = server.takeRequest() //Post to oauth2/realms/root/token/revoke
        val revoke2 = server.takeRequest() //Post to /endSession
        Assertions.assertThat(result["END_SESSION"]?.second).isEqualTo(1)
    }

    private fun parse(encoded: String): Map<String, String> {
        val body = encoded.split("&").toTypedArray()
        val result: MutableMap<String, String> = HashMap()
        for (s in body) {
            val value = s.split("=").toTypedArray()
            result[value[0]] = value[1]
        }
        return result
    }

    private fun findRequest(
        path: String,
        vararg recordedRequests: RecordedRequest
    ): RecordedRequest {
        for (r in recordedRequests) {
            if (r.path.startsWith(path)) {
                return r
            }
        }
        throw IllegalArgumentException()
    }

    companion object {
        private const val DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest"
        private const val DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest"
        const val INVALID_SCOPE = "Invalid Scope"
    }
}