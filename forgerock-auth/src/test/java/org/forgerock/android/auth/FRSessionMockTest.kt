/*
 * Copyright (c) 2019 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.net.Uri
import android.os.OperationCanceledException
import android.util.Pair
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.PolicyAdvice.Companion.builder
import org.forgerock.android.auth.callback.NameCallback
import org.forgerock.android.auth.callback.PasswordCallback
import org.forgerock.android.auth.callback.SuspendedTextOutputCallback
import org.forgerock.android.auth.exception.SuspendedAuthSessionException
import org.forgerock.android.auth.storage.Storage
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.util.concurrent.ExecutionException

@RunWith(AndroidJUnit4::class)
class FRSessionMockTest : BaseTest() {

    private lateinit var ssoTokenStorage: Storage<SSOToken>
    private lateinit var cookiesStorage: Storage<Collection<String>>

    @Before
    fun setUpStorage() {
        ssoTokenStorage = sharedPreferencesStorage<SSOToken>(context = context,
            filename = "ssotoken",
            key = "ssotoken", cacheable = false)
        cookiesStorage = sharedPreferencesStorage<Collection<String>>(context = context,
            filename = "cookies",
            key = "cookies", cacheable = false)

    }

    @After
    @Throws(Exception::class)
    fun closeSession() {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout()
        }
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun frSessionHappyPath() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().url = url
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage

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
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun frSessionWithPolicyAdvice() {
        //First Round

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        //Second Round
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().url = url
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage


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

        val advice = builder()
            .type("TransactionConditionAdvice")
            .value("3b8c1b2b-0aed-461a-a49b-f35da8276d12").build()

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        Assert.assertTrue(nodeListenerFuture.get() is FRSession)
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)

        nodeListenerFuture.reset()
        FRSession.getCurrentSession().authenticate(context, advice, nodeListenerFuture)
        Assert.assertTrue(nodeListenerFuture.get() is FRSession)

        server.takeRequest()
        server.takeRequest()
        server.takeRequest()

        val recordedRequest = server.takeRequest() //The one with step up
        val uri = Uri.parse(recordedRequest.path)
        Assertions.assertThat(uri.getQueryParameter("authIndexType")).isEqualTo("composite_advice")
        Assertions.assertThat(uri.getQueryParameter("authIndexValue")).isEqualTo(advice.toString())

        //Make sure we have sent the request.
        server.takeRequest()
        server.takeRequest()
    }

    @Test
    @Throws(Exception::class)
    fun testFRSessionReAuthenticate() {
        //First Round

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        //Second Round
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().url = url
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage

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

        nodeListenerFuture.reset()

        FRSession.authenticate(context, "Example", nodeListenerFuture)

        Assert.assertTrue(nodeListenerFuture.get() is FRSession)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testWithNoSession() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success_withNoSession.json",
            HttpURLConnection.HTTP_OK)

        Config.getInstance().url = url
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage


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

        RequestInterceptorRegistry.getInstance()
            .register(FRRequestInterceptor { request: Request, tag: Action ->
                if (tag.type == Action.AUTHENTICATE) {
                    return@FRRequestInterceptor request.newBuilder()
                        .url(Uri.parse(request.url().toString())
                            .buildUpon()
                            .appendQueryParameter("noSession", "true").toString())
                        .build()
                }
                request
            } as FRRequestInterceptor<Action>)

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        Assertions.assertThat(nodeListenerFuture.get()).isNull()
        Assertions.assertThat(FRSession.getCurrentSession()).isNull()
        Assertions.assertThat(FRUser.getCurrentUser()).isNull()

        var recordedRequest = server.takeRequest() //NameCallback
        recordedRequest = server.takeRequest() //PasswordCallback without Session
        Assertions.assertThat(Uri.parse(recordedRequest.path).getQueryParameter("noSession"))
            .isEqualTo("true")
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testWithSessionThenWithoutSession() {
        frSessionHappyPath()

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success_withNoSession.json",
            HttpURLConnection.HTTP_OK)

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

        RequestInterceptorRegistry.getInstance()
            .register(FRRequestInterceptor { request: Request, tag: Action ->
                if (tag.type == Action.AUTHENTICATE) {
                    return@FRRequestInterceptor request.newBuilder()
                        .url(Uri.parse(request.url().toString())
                            .buildUpon()
                            .appendQueryParameter("noSession", "true").toString())
                        .build()
                }
                request
            } as FRRequestInterceptor<Action>)

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        Assertions.assertThat(nodeListenerFuture.get())
            .isNull() //The without session oen should return null
        //Retrieve the previous session
        Assert.assertNotNull(FRSession.getCurrentSession()) //Retrieve the previous Session
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)

        var recordedRequest = server.takeRequest() //NameCallback with Session
        recordedRequest = server.takeRequest() //PasswordCallback with Session
        recordedRequest = server.takeRequest() //End of tree with Session

        recordedRequest = server.takeRequest() //NameCallback without Session
        recordedRequest = server.takeRequest() //PasswordCallback without Session
        recordedRequest = server.takeRequest() //PasswordCallback without Session
        Assertions.assertThat(Uri.parse(recordedRequest.path).getQueryParameter("noSession"))
            .isEqualTo("true")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNoSuspendId() {
        FRSession.authenticate(context, Uri.parse("http://dummy/"), null)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testWithSuspendedEmail() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_EmailSuspended.json", HttpURLConnection.HTTP_OK)

        Config.getInstance().url = url

        val suspended = booleanArrayOf(false)

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

                if (state.getCallback<SuspendedTextOutputCallback?>(SuspendedTextOutputCallback::class.java) != null) {
                    suspended[0] = true
                    this.onException(OperationCanceledException())
                }
            }
        }

        FRSession.authenticate(context, "Example", nodeListenerFuture)
        try {
            nodeListenerFuture.get()
            Assertions.fail("Should throw exception")
        } catch (e: ExecutionException) {
            Assertions.assertThat(e.cause).isInstanceOf(OperationCanceledException::class.java)
        }
        Assertions.assertThat(FRSession.getCurrentSession()).isNull()
        Assertions.assertThat(suspended[0]).isTrue()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testWithResumeUri() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

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

        Config.getInstance().url = url
        Config.getInstance().ssoTokenStorage = ssoTokenStorage
        Config.getInstance().cookiesStorage = cookiesStorage

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

        FRSession.authenticate(context,
            Uri.parse("http://openam.example.com:8081/openam/XUI?realm=/&suspendedId=YGJ1o1snV96U6u7XT8SaHhX4Cv8"),
            nodeListenerFuture)


        Assertions.assertThat(nodeListenerFuture.get()).isInstanceOf(
            FRSession::class.java)
        Assertions.assertThat(
            result["RESUME_AUTHENTICATE"]).isNotNull()
        Assertions.assertThat(FRSession.getCurrentSession()).isNotNull()
        Assertions.assertThat(FRSession.getCurrentSession().sessionToken).isNotNull()
        val recordedRequest = server.takeRequest()
        Assertions.assertThat(recordedRequest.path)
            .isEqualTo("/json/realms/root/authenticate?suspendedId=YGJ1o1snV96U6u7XT8SaHhX4Cv8")
        Assertions.assertThat(recordedRequest.method).isEqualTo("POST")
    }

    @Test
    @Throws(InterruptedException::class)
    fun testWithExpiredSuspendedId() {
        enqueue("/authTreeMockTest_Authenticate_Expired_SuspendedId.json",
            HttpURLConnection.HTTP_UNAUTHORIZED)

        Config.getInstance().url = url
        val nodeListenerFuture: NodeListenerFuture<FRSession> = object : NodeListenerFuture<FRSession>() {
            override fun onCallbackReceived(state: Node) {
            }
        }

        FRSession.authenticate(context,
            Uri.parse("http://openam.example.com:8081/openam/XUI?realm=/&suspendedId=YGJ1o1snV96U6u7XT8SaHhX4Cv8"),
            nodeListenerFuture)


        try {
            nodeListenerFuture.get()
            Assertions.fail("Should throw exception")
        } catch (e: ExecutionException) {
            Assertions.assertThat(e.cause).isInstanceOf(
                SuspendedAuthSessionException::class.java)
        }
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testLogout() {
        frSessionHappyPath()
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK)

        FRSession.getCurrentSession().logout()

        //Check SSOToken Storage
        val singleSignOnManager: SingleSignOnManager = DefaultSingleSignOnManager.builder()
            .context(context)
            .build()

        Assertions.assertThat(singleSignOnManager.token).isNull()

        Assertions.assertThat(FRSession.getCurrentSession()).isNull()
    }

    companion object {
        private const val DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest"
    }
}
