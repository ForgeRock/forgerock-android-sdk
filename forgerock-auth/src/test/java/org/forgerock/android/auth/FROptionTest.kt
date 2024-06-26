/*
 * Copyright (c) 2022 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.forgerock.android.auth.exception.ApiException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection

@RunWith(AndroidJUnit4::class)
class FROptionTest: BaseTest()  {
    @Test
    fun testDefaultBuilderOption() {
        class TestCustomLogger: FRLogger {
            override fun error(tag: String?, t: Throwable?, message: String?, vararg values: Any?) {}
            override fun error(tag: String?, message: String?, vararg values: Any?) {}
            override fun warn(tag: String?, message: String?, vararg values: Any?) {}
            override fun warn(tag: String?, t: Throwable?, message: String?, vararg values: Any?) {}
            override fun debug(tag: String?, message: String?, vararg values: Any?) {}
            override fun info(tag: String?, message: String?, vararg values: Any?) {}
            override fun network(tag: String?, message: String?, vararg values: Any?) {}
        }
        val logger:FRLogger  = TestCustomLogger()
        val option = FROptionsBuilder.build {
            server {
                url = "https://andy.com"
                realm = "root"
            }
            logger {
                logLevel = Logger.Level.ERROR
                customLogger = logger
            }
        }
        assertTrue(option.sslPinning.pins == emptyList<String>())
        assertTrue(option.sslPinning.buildSteps == emptyList<BuildStep<OkHttpClient.Builder>>())
        assertTrue(option.server.realm == "root")
        assertTrue(option.server.url == "https://andy.com")
        assertTrue(option.server.cookieName == "iPlanetDirectoryPro")
        assertTrue(option.server.cookieCacheSeconds == 0.toLong())
        assertTrue(option.server.timeout == 30)
        assertTrue(option.oauth.oauthClientId == "")
        assertTrue(option.oauth.oauthRedirectUri == "")
        assertTrue(option.oauth.oauthScope == "")
        assertTrue(option.oauth.oauthThresholdSeconds == 0.toLong())
        assertTrue(option.oauth.oauthCacheSeconds == 0.toLong())
        assertTrue(option.service.authServiceName == "Login")
        assertTrue(option.service.registrationServiceName == "Registration")
        assertTrue(option.urlPath.authorizeEndpoint == null)
        assertTrue(option.urlPath.revokeEndpoint == null)
        assertTrue(option.urlPath.sessionEndpoint == null)
        assertTrue(option.urlPath.tokenEndpoint == null)
        assertTrue(option.urlPath.userinfoEndpoint == null)
        assertTrue(option.urlPath.authorizeEndpoint == null)
        assertTrue(option.urlPath.endSessionEndpoint == null)
        assertTrue(option.logger.logLevel == Logger.Level.ERROR)
        assertTrue(option.logger.customLogger == logger)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInValidConfigRealm() {
        val option1 = FROptionsBuilder.build { server {
            url = "https://stoyan.com"
            realm = ""
        }}
        option1.validateConfig()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInValidConfigCookieName() {
        val option1 = FROptionsBuilder.build { server {
            url = "https://stoyan.com"
            cookieName = ""
        }}
        option1.validateConfig()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInValidConfigUrl() {
        val option1 = FROptionsBuilder.build { server {
            url = ""
        }}
        option1.validateConfig()
    }

    @Test
    fun testReferenceAndValue() {
        val option1 = FROptionsBuilder.build { server {
            url = "https://stoyan.com"
        }}
        var option2 = FROptionsBuilder.build { server {
            url = "https://stoyan.com"
        }}
        assertTrue(FROptions.equals(option1, option2))
        option2 = FROptionsBuilder.build { server {
            url = "https://andy.com"
            realm = ""
        }}
        assertFalse(FROptions.equals(option1, option2))
        assertTrue(FROptions.equals(option2, option2))
    }

    @Test
    fun testDiscoverEndpointFailure() {
        runBlocking {
            val option1 = FROptionsBuilder.build {
                server {
                    url = "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as"
                }
                urlPath {
                    authenticateEndpoint = "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/authenticate"
                    sessionEndpoint = "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/session"
                }
                oauth {
                    oauthClientId = "AndroidTest"
                    oauthRedirectUri = "org.forgerock.demo:/oauth2redirect"
                    oauthScope = "openid profile email address phone"
                }
            }

            server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_FORBIDDEN))

            try {
                val copied =
                    option1.discover(url)
                FRAuth.start(context, copied)
                fail()
            }
            catch (e: ApiException) {
                assertTrue(e.statusCode == HttpURLConnection.HTTP_FORBIDDEN)
            }
        }
    }
    @Test
    fun testDiscoverEndpointWithURLProvided() {
        runBlocking {
            server.enqueue(
                MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody(getJson("/discovery.json")))

            val option1 = FROptionsBuilder.build {
                server {
                    url = "https://auth.pingone.us/02fb4743-189a-4bc7-9d6c-a919edfe6447/as"
                }
                urlPath {
                    authenticateEndpoint = "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/authenticate"
                    sessionEndpoint = "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/session"
                }
                oauth {
                    oauthClientId = "AndroidTest"
                    oauthRedirectUri = "org.forgerock.demo:/oauth2redirect"
                    oauthScope = "openid profile email address phone"
                }
            }
            val copied = option1.discover(url)
            assertTrue(copied.urlPath.authorizeEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/authorize")
            assertTrue(copied.server.url == "https://auth.pingone.us/02fb4743-189a-4bc7-9d6c-a919edfe6447/as")
            assertTrue(copied.urlPath.revokeEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/revoke")
            assertTrue(copied.urlPath.tokenEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/token")
            assertTrue(copied.urlPath.userinfoEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/userinfo")
            assertTrue(copied.urlPath.endSessionEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/signoff")
            assertTrue(copied.urlPath.sessionEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/session")
            assertTrue(copied.urlPath.authenticateEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/authenticate")
            assertTrue(copied.oauth.oauthClientId == "AndroidTest")
            assertTrue(copied.oauth.oauthRedirectUri == "org.forgerock.demo:/oauth2redirect")
            assertTrue(copied.oauth.oauthScope == "openid profile email address phone")
        }
    }

    @Test
    fun testDiscoverEndpointWithURLNotProvided() {
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(HttpURLConnection.HTTP_OK)
                    .addHeader("Content-Type", "application/json")
                    .setBody(getJson("/discovery.json")))

            val option1 = FROptionsBuilder.build {
                urlPath {
                    authenticateEndpoint = "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/authenticate"
                    sessionEndpoint = "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/session"
                }
                oauth {
                    oauthClientId = "AndroidTest"
                    oauthRedirectUri = "org.forgerock.demo:/oauth2redirect"
                    oauthScope = "openid profile email address phone"
                }
            }
            val copied = option1.discover(url)
            assertTrue(copied.urlPath.authorizeEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/authorize")
            assertTrue(copied.server.url == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as")
            assertTrue(copied.urlPath.revokeEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/revoke")
            assertTrue(copied.urlPath.tokenEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/token")
            assertTrue(copied.urlPath.userinfoEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/userinfo")
            assertTrue(copied.urlPath.endSessionEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/signoff")
            assertTrue(copied.urlPath.sessionEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/session")
            assertTrue(copied.urlPath.authenticateEndpoint == "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as/authenticate")
            assertTrue(copied.oauth.oauthClientId == "AndroidTest")
            assertTrue(copied.oauth.oauthRedirectUri == "org.forgerock.demo:/oauth2redirect")
            assertTrue(copied.oauth.oauthScope == "openid profile email address phone")
        }
    }


}