/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FROptionTest {
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

    @Test
    fun testReferenceAndValue() {
        val option1 = FROptionsBuilder.build {  }
        var option2 = FROptionsBuilder.build {  }
        assertTrue(FROptions.equals(option1, option2))
        option2 = FROptionsBuilder.build { server {
            url = "https://andy.com"
            realm = ""
        }}
        assertFalse(FROptions.equals(option1, option2))
        assertTrue(FROptions.equals(option2, option2))
    }
}