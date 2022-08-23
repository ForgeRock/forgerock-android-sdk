/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigHelperTest {

    val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testPersistingData() {

        val frOptions = FROptionsBuilder.build {
            server {
                url = "https://dummy"
                realm = "realm123"
                cookieName = "cookieName"
            }
            oauth {
                oauthClientId = "client_id"
                oauthScope = "openid email address"
                oauthRedirectUri = "https://redirecturi"
            }
            urlPath {
                revokeEndpoint = "https://revoke"
                endSessionEndpoint = "https://endsession"
                sessionEndpoint = "https://logout"
            }
        }
        ConfigHelper.persist(context, frOptions)
        val sharedPreferences =
            context.getSharedPreferences(ConfigHelper.ORG_FORGEROCK_V_1_HOSTS, Context.MODE_PRIVATE)
        assertTrue(sharedPreferences.getString("url", null) == "https://dummy")
        assertTrue(sharedPreferences.getString("realm", null) == "realm123")
        assertTrue(sharedPreferences.getString("cookieName", null) == "cookieName")
        assertTrue(sharedPreferences.getString("client_id", null) == "client_id")
        assertTrue(sharedPreferences.getString("revoke_endpoint", null) == "https://revoke")
        assertTrue(sharedPreferences.getString("end_session_endpoint", null) == "https://endsession")
        assertTrue(sharedPreferences.getString("session_endpoint", null) == "https://logout")
        assertTrue(sharedPreferences.getString("scope", null) == "openid email address")
        assertTrue(sharedPreferences.getString("redirect_uri", null) == "https://redirecturi")
    }

    @Test
    fun testConfigChanged() {
        val frOptions = FROptionsBuilder.build {
            server {
                url = "https://dummy"
                realm = "realm123"
                cookieName = "cookieName"
            }
            oauth {
                oauthClientId = "client_id"
                oauthScope = "scope"
                oauthRedirectUri = "redirecturi"
            }
            urlPath {
                revokeEndpoint = "https://revoke"
                endSessionEndpoint = "https://endsession"
            }
        }
        val cookieChanged = FROptionsBuilder.build {
            server {
                url = ""
                realm = ""
                cookieName = "cookieName1"
            }
        }
        val realmChanged = FROptionsBuilder.build {
            server {
                url = ""
                realm = ""
                realm = "realm1"
            }
        }
        val clientChanged = FROptionsBuilder.build {
            server {
                url = ""
                realm = ""
                cookieName = "cookieName1"
            }
            oauth {
               oauthClientId  = "clientId"
            }
        }
        val redirectURIChanged = FROptionsBuilder.build {
            server {
                url = ""
                realm = ""
                cookieName = "cookieName1"
            }
            oauth {
                oauthRedirectUri  = "https://redirectURI"
            }
        }
        val scopeChanged = FROptionsBuilder.build {
            server {
                url = ""
                realm = ""
                cookieName = "cookieName1"
            }
            oauth {
                oauthScope  = "scope"
            }
        }
        val urlChanged = FROptionsBuilder.build {
            server {
                url  = "dummy"
                realm = ""
            }
        }
        ConfigHelper.persist(context, frOptions)
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, cookieChanged))
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, realmChanged))
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, clientChanged))
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, urlChanged))
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, redirectURIChanged))
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, scopeChanged))
        assertFalse(ConfigHelper.isConfigDifferentFromPersistedValue(context, frOptions))

        val preference = ConfigHelper.loadFromPreference(context)
        assertTrue(preference == frOptions)
    }

    @Test
    fun closeSession() {
        val config = ConfigHelper.getPersistedConfig(context, null)
        assertNotNull(config)
    }

    @Test
    fun loadDefaultFROptionWithNull() {
       val defaultOption = ConfigHelper.load(context, null)
       val expectedResult = "FROptions(server=Server(url=https://openam.example.com:8081/openam, realm=root, timeout=30, cookieName=iPlanetDirectoryPro, cookieCacheSeconds=0), oauth=OAuth(oauthClientId=andy_app, oauthRedirectUri=https://www.example.com:8080/callback, oauthScope=openid email address, oauthThresholdSeconds=30, oauthCacheSeconds=0), service=Service(authServiceName=Test, registrationServiceName=Registration), urlPath=UrlPath(authenticateEndpoint=, revokeEndpoint=, sessionEndpoint=, tokenEndpoint=, userinfoEndpoint=, authorizeEndpoint=, endSessionEndpoint=), sslPinning=SSLPinning(buildSteps=[], pins=[9hNxmEFgLKGJXqgp61hyb8yIyiT9u0vgDZh4y8TmY/M=]), logger=Log(logLevel=null, customLogger=null))"
       assertTrue(defaultOption.toString() == expectedResult)
    }

    @Test
    fun loadFROption() {
        val frOptions = FROptionsBuilder.build {
            server {
                url = "https://dummy"
                realm = "realm123"
                cookieName = "cookieName"
            }
            oauth {
                oauthClientId = "client_id"
            }
            urlPath {
                revokeEndpoint = "https://revoke"
                endSessionEndpoint = "https://endsession"
            }
        }
        val defaultOption = ConfigHelper.load(context, frOptions)
        val expectedResult = "FROptions(server=Server(url=https://dummy, realm=realm123, timeout=30, cookieName=cookieName, cookieCacheSeconds=0), oauth=OAuth(oauthClientId=client_id, oauthRedirectUri=, oauthScope=, oauthThresholdSeconds=0, oauthCacheSeconds=0), service=Service(authServiceName=Login, registrationServiceName=Registration), urlPath=UrlPath(authenticateEndpoint=null, revokeEndpoint=https://revoke, sessionEndpoint=null, tokenEndpoint=null, userinfoEndpoint=null, authorizeEndpoint=null, endSessionEndpoint=https://endsession), sslPinning=SSLPinning(buildSteps=[], pins=[]), logger=Log(logLevel=null, customLogger=null))"
        assertTrue(defaultOption.toString() == expectedResult)
    }
}