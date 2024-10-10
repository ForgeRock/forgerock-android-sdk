/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.content.res.Resources
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.forgerock.android.auth.callback.NameCallback
import org.forgerock.android.auth.callback.PasswordCallback
import org.forgerock.android.auth.exception.AuthenticationRequiredException
import org.forgerock.android.auth.storage.Storage
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.net.HttpURLConnection
import java.util.concurrent.ExecutionException

@RunWith(AndroidJUnit4::class)
class FRAuthMockTest : BaseTest() {

    private lateinit var ssoTokenStorage: Storage<SSOToken>
    private lateinit var cookiesStorage: Storage<Collection<String>>

    @Before
    @Throws(Exception::class)
    fun setUp() {
        ssoTokenStorage = sharedPreferencesStorage<SSOToken>(context = context,
            filename = "ssoToken",
            key = "ssoToken", cacheable = false)
        cookiesStorage = sharedPreferencesStorage<Collection<String>>(context = context,
            filename = "cookies",
            key = "cookies", cacheable = false)

    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        ssoTokenStorage.delete()
        cookiesStorage.delete()
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun frAuthHappyPath() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        val singleSignOnManager: SingleSignOnManager = DefaultSingleSignOnManager.builder()
            .ssoTokenStorage(ssoTokenStorage)
            .cookiesStorage(cookiesStorage)
            .serverConfig(serverConfig)
            .context(context)
            .build()

        val frAuth = FRAuth.builder()
            .serviceName("Example")
            .context(context)
            .sessionManager(SessionManager.builder()
                .tokenManager(Config.getInstance().tokenManager)
                .singleSignOnManager(singleSignOnManager)
                .build())
            .serverConfig(serverConfig)
            .build()

        val nodeListenerFuture: NodeListenerFuture<*> = object : NodeListenerFuture<Any?>() {
            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(NameCallback::class.java) != null) {
                    node.getCallback(NameCallback::class.java).setName("tester")
                    node.next(context, this)
                    return
                }

                if (node.getCallback(PasswordCallback::class.java) != null) {
                    node.getCallback(PasswordCallback::class.java)
                        .setPassword("password".toCharArray())
                    node.next(context, this)
                }
            }
        }

        frAuth.next(context, nodeListenerFuture)

        Assert.assertTrue(nodeListenerFuture.get() is SSOToken)
    }

    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun frAuthHappyPathWithConfig() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        // Mocking the config
        val resources = Mockito.mock(
            Resources::class.java)
        Mockito.`when`(mockContext.resources).thenReturn(resources)
        Mockito.`when`(mockContext.applicationContext).thenReturn(mockContext)
        Mockito.`when`(resources.getInteger(R.integer.forgerock_timeout)).thenReturn(30)

        Mockito.`when`(mockContext.getString(R.string.forgerock_url)).thenReturn(url)
        Mockito.`when`(mockContext.getString(R.string.forgerock_oauth_url)).thenReturn(
            url)
        Mockito.`when`(mockContext.getString(R.string.forgerock_realm)).thenReturn("root")
        Mockito.`when`(mockContext.getString(R.string.forgerock_oauth_redirect_uri))
            .thenReturn("http://www.example.com:8080/callback")
        Mockito.`when`(mockContext.getString(R.string.forgerock_oauth_scope)).thenReturn("openid")
        Mockito.`when`(mockContext.getString(R.string.forgerock_oauth_client_id))
            .thenReturn("andy_app")

        Config.reset()
        Config.getInstance().init(context, null)

        val singleSignOnManager: SingleSignOnManager = DefaultSingleSignOnManager.builder()
            .ssoTokenStorage(ssoTokenStorage)
            .cookiesStorage(cookiesStorage)
            .context(context)
            .build()

        val frAuth = FRAuth.builder()
            .serviceName("Example")
            .context(mockContext)
            .sessionManager(SessionManager.builder()
                .tokenManager(Config.getInstance().tokenManager)
                .singleSignOnManager(singleSignOnManager)
                .build())
            .serverConfig(serverConfig)
            .build()

        val nodeListenerFuture: NodeListenerFuture<*> = object : NodeListenerFuture<Any?>() {
            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(NameCallback::class.java) != null) {
                    node.getCallback(NameCallback::class.java).setName("tester")
                    node.next(mockContext, this)
                    return
                }

                if (node.getCallback(PasswordCallback::class.java) != null) {
                    node.getCallback(PasswordCallback::class.java)
                        .setPassword("password".toCharArray())
                    node.next(mockContext, this)
                }
            }
        }

        frAuth.next(mockContext, nodeListenerFuture)

        Assert.assertTrue(nodeListenerFuture.get() is SSOToken)

        var rr = server.takeRequest() //Start the Auth Service
        Assert.assertEquals("/json/realms/root/authenticate?authIndexType=service&authIndexValue=Example",
            rr.path)

        rr = server.takeRequest() //Post Name Callback
        Assert.assertEquals("/json/realms/root/authenticate", rr.path)

        rr = server.takeRequest() //Post Password Callback
        Assert.assertEquals("/json/realms/root/authenticate", rr.path)
    }

    @Test
    @Throws(ExecutionException::class,
        InterruptedException::class,
        AuthenticationRequiredException::class)
    fun testWithSharedPreferencesSSOManager() {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)

        val singleSignOnManager: SingleSignOnManager = SharedPreferencesSignOnManager(context,
            context.getSharedPreferences("Test", Context.MODE_PRIVATE))

        val frAuth = FRAuth.builder()
            .serviceName("Example")
            .context(context)
            .sessionManager(SessionManager.builder()
                .tokenManager(Config.getInstance().tokenManager)
                .singleSignOnManager(singleSignOnManager)
                .build())
            .serverConfig(serverConfig)
            .build()

        val nodeListenerFuture: NodeListenerFuture<*> = object : NodeListenerFuture<Any?>() {
            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(NameCallback::class.java) != null) {
                    node.getCallback(NameCallback::class.java).setName("tester")
                    node.next(context, this)
                    return
                }

                if (node.getCallback(PasswordCallback::class.java) != null) {
                    node.getCallback(PasswordCallback::class.java)
                        .setPassword("password".toCharArray())
                    node.next(context, this)
                }
            }
        }

        frAuth.next(context, nodeListenerFuture)

        Assert.assertTrue(nodeListenerFuture.get() is SSOToken)

        //Check SSOToken Storage
        val token: SSOToken = singleSignOnManager.token
        Assert.assertNotNull(token)
    }

    @Test
    fun startTest() {
        val sharedPreferences =
            context.getSharedPreferences(ConfigHelper.ORG_FORGEROCK_V_1_HOSTS, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("url", "https://somethingelse").commit()

        FRAuth.start(context)

        //host url is created
        Assert.assertEquals(Config.getInstance().url, sharedPreferences.getString("url", null))
        Assert.assertEquals(Config.getInstance().realm, sharedPreferences.getString("realm", null))
        Assert.assertEquals(Config.getInstance().cookieName,
            sharedPreferences.getString("cookieName", null))
        Assert.assertEquals(Config.getInstance().clientId,
            sharedPreferences.getString("client_id", null))
        Assert.assertEquals(Config.getInstance().scope, sharedPreferences.getString("scope", null))
        Assert.assertEquals(Config.getInstance().redirectUri,
            sharedPreferences.getString("redirect_uri", null))

        FRAuth.start(context, null)

        //host url is created
        Assert.assertEquals(Config.getInstance().url, sharedPreferences.getString("url", null))
        Assert.assertEquals(Config.getInstance().realm, sharedPreferences.getString("realm", null))
        Assert.assertEquals(Config.getInstance().cookieName,
            sharedPreferences.getString("cookieName", null))
        Assert.assertEquals(Config.getInstance().clientId,
            sharedPreferences.getString("client_id", null))
        Assert.assertEquals(Config.getInstance().scope, sharedPreferences.getString("scope", null))
        Assert.assertEquals(Config.getInstance().redirectUri,
            sharedPreferences.getString("redirect_uri", null))
    }

    @Test
    fun startTestWithOptionsAndNull() {
        val sharedPreferences =
            context.getSharedPreferences(ConfigHelper.ORG_FORGEROCK_V_1_HOSTS, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("url", "https://somethingelseone").commit()

        val frOptions =  FROptionsBuilder.build {
            server {
                url = "https://forgerocker.com"
                realm = "realm"
                cookieName = "planet"
                cookieCacheSeconds = 5000
                timeout = 50
            }
            oauth {
                oauthClientId = "client_id"
                oauthRedirectUri = "https://oauth2redirect"
                oauthCacheSeconds = 0
                oauthScope = "scope"
                oauthThresholdSeconds = 5000
            }
            urlPath {
                revokeEndpoint = "https://revokeEndpoint.com"
                endSessionEndpoint = "https://endSessionEndpoint.com"
                authenticateEndpoint = "https://authenticateEndpoint.com"
                authorizeEndpoint = "https://authorizeEndpoint.com"
                sessionEndpoint = "https://logoutEndpoint.com"
                tokenEndpoint = "https://tokenEndpoint.com"
            }
            service {
                authServiceName = "WebAuthn"
            }
            sslPinning {
                buildSteps = emptyList()
                pins = emptyList()
            }

            service {
                authServiceName = "auth_service"
                registrationServiceName = "reg_service"
            }
        }

        FRAuth.start(context, frOptions)
        //host url is created
        Assert.assertEquals(Config.getInstance().url, sharedPreferences.getString("url", null))
        Assert.assertEquals(Config.getInstance().realm, sharedPreferences.getString("realm", null))
        Assert.assertEquals(Config.getInstance().cookieName,
            sharedPreferences.getString("cookieName", null))
        Assert.assertEquals(Config.getInstance().clientId,
            sharedPreferences.getString("client_id", null))
        Assert.assertEquals(Config.getInstance().scope, sharedPreferences.getString("scope", null))
        Assert.assertEquals(Config.getInstance().redirectUri,
            sharedPreferences.getString("redirect_uri", null))

        Assert.assertEquals(Config.getInstance().pins, frOptions.sslPinning.pins)
        Assert.assertEquals(Config.getInstance().buildSteps, frOptions.sslPinning.buildSteps)
        Assert.assertEquals(Config.getInstance().authServiceName, frOptions.service.authServiceName)
        Assert.assertEquals(Config.getInstance().registrationServiceName,
            frOptions.service.registrationServiceName)

        Assert.assertEquals(Config.getInstance().cookieName, frOptions.server.cookieName)
        val millisCookieCache = frOptions.server.cookieCacheSeconds * 1000
        Assert.assertEquals(Config.getInstance().cookieCacheMillis, millisCookieCache)

        Assert.assertEquals(Config.getInstance().realm, frOptions.server.realm)
        Assert.assertEquals(Config.getInstance().timeout.toLong(),
            frOptions.server.timeout.toLong())
        Assert.assertEquals(Config.getInstance().url, frOptions.server.url)

        Assert.assertEquals(Config.getInstance().redirectUri, frOptions.oauth.oauthRedirectUri)
        Assert.assertEquals(Config.getInstance().scope, frOptions.oauth.oauthScope)
        Assert.assertEquals(Config.getInstance().clientId, frOptions.oauth.oauthClientId)
        val millisOauthCache = frOptions.oauth.oauthCacheSeconds * 1000
        Assert.assertEquals(Config.getInstance().oauthCacheMillis, millisOauthCache)
        val thresholdSeconds = frOptions.oauth.oauthThresholdSeconds
        Assert.assertEquals(Config.getInstance().oauthThreshold, thresholdSeconds)

        FRAuth.start(context, null)
        val sharedPreferences1 =
            context.getSharedPreferences(ConfigHelper.ORG_FORGEROCK_V_1_HOSTS, Context.MODE_PRIVATE)
        //host url is created
        Assert.assertEquals(Config.getInstance().url, sharedPreferences1.getString("url", null))
        Assert.assertEquals(Config.getInstance().realm, sharedPreferences1.getString("realm", null))
        Assert.assertEquals(Config.getInstance().cookieName,
            sharedPreferences1.getString("cookieName", null))
        Assert.assertEquals(Config.getInstance().clientId,
            sharedPreferences1.getString("client_id", null))
        Assert.assertEquals(Config.getInstance().scope, sharedPreferences1.getString("scope", null))
        Assert.assertEquals(Config.getInstance().redirectUri,
            sharedPreferences1.getString("redirect_uri", null))


        Assert.assertEquals(Config.getInstance().pins,
            listOf("9hNxmEFgLKGJXqgp61hyb8yIyiT9u0vgDZh4y8TmY/M="))
        Assert.assertEquals(Config.getInstance().buildSteps, emptyList<Any>())
        Assert.assertEquals(Config.getInstance().authServiceName, "Test")
        Assert.assertEquals(Config.getInstance().registrationServiceName, "Registration")

        Assert.assertEquals(Config.getInstance().cookieName, "iPlanetDirectoryPro")
        Assert.assertEquals(Config.getInstance().realm, "root")
        Assert.assertEquals(Config.getInstance().timeout.toLong(), 30)
        Assert.assertEquals(Config.getInstance().url, "https://openam.example.com:8081/openam")

        Assert.assertEquals(Config.getInstance().redirectUri,
            "https://www.example.com:8080/callback")
        Assert.assertEquals(Config.getInstance().scope, "openid email address")
        Assert.assertEquals(Config.getInstance().clientId, "andy_app")
        Assert.assertEquals(Config.getInstance().oauthCacheMillis, 0)
        Assert.assertEquals(Config.getInstance().oauthThreshold, 30)
        Assert.assertEquals(Config.getInstance().cookieCacheMillis, 0)
    }
}
