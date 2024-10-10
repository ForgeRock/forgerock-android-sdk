/*
 * Copyright (c) 2022 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigHelperTest {

    val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        ContextProvider.init(context)
    }

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
                oauthSignOutRedirectUri = "https://signout"
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
        assertTrue(sharedPreferences.getString("sign_out_redirect_uri", null) == "https://signout")
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
                oauthSignOutRedirectUri = "https://signout"
            }
            urlPath {
                revokeEndpoint = "https://revoke"
                endSessionEndpoint = "https://endsession"
                sessionEndpoint = "https://sessionEndpoint"
            }
        }
        val cookieChanged = FROptionsBuilder.build {
            server {
                url = "https://dummy"
                realm = "realm123"
                cookieName = "cookieName1"
            }
            oauth {
                oauthClientId = "client_id"
                oauthScope = "scope"
                oauthRedirectUri = "redirecturi"
                oauthSignOutRedirectUri = "https://signout"
            }
            urlPath {
                revokeEndpoint = "https://revoke"
                endSessionEndpoint = "https://endsession"
            }
        }
        val realmChanged = FROptionsBuilder.build {
            server {
                url = "https://dummy"
                realm = "realm1234"
                cookieName = "cookieName"
            }
            oauth {
                oauthClientId = "client_id"
                oauthScope = "scope"
                oauthRedirectUri = "redirecturi"
                oauthSignOutRedirectUri = "https://signout"
            }
            urlPath {
                revokeEndpoint = "https://revoke"
                endSessionEndpoint = "https://endsession"
            }
        }
        val clientChanged = FROptionsBuilder.build {
            server {
                url = "https://dummy"
                realm = "realm123"
                cookieName = "cookieName"
            }
            oauth {
                oauthClientId = "client_id_1"
                oauthScope = "scope"
                oauthRedirectUri = "redirecturi"
                oauthSignOutRedirectUri = "https://signout"
            }
            urlPath {
                revokeEndpoint = "https://revoke"
                endSessionEndpoint = "https://endsession"
            }
        }
        val redirectURIChanged = FROptionsBuilder.build {
            server {
                url = "https://dummy"
                realm = "realm123"
                cookieName = "cookieName"
            }
            oauth {
                oauthClientId = "client_id"
                oauthScope = "scope"
                oauthRedirectUri = "redirecturi_uri"
                oauthSignOutRedirectUri = "https://signout"
            }
            urlPath {
                revokeEndpoint = "https://revoke"
                endSessionEndpoint = "https://endsession"
            }
        }
        val signOutRedirectUriChanged = FROptionsBuilder.build {
            server {
                url = "https://dummy"
                realm = "realm123"
                cookieName = "cookieName"
            }
            oauth {
                oauthClientId = "client_id"
                oauthScope = "scope"
                oauthRedirectUri = "redirecturi_uri"
                oauthSignOutRedirectUri = "https://signout_changed"
            }
            urlPath {
                revokeEndpoint = "https://revoke"
                endSessionEndpoint = "https://endsession"
            }
        }
        val scopeChanged = FROptionsBuilder.build {
            server {
                url = "https://dummy"
                realm = "realm123"
                cookieName = "cookieName"
            }
            oauth {
                oauthClientId = "client_id"
                oauthScope = "scope_test"
                oauthRedirectUri = "redirecturi"
                oauthSignOutRedirectUri = "https://signout"
            }
            urlPath {
                revokeEndpoint = "https://revoke"
                endSessionEndpoint = "https://endsession"
            }
        }
        val expectedURL = FROptionsBuilder.build {
            server {
                url = "https://dummynew"
                realm = "realm123"
                cookieName = "cookieName"
            }
            oauth {
                oauthClientId = "client_id"
                oauthScope = "scope"
                oauthRedirectUri = "redirecturi"
                oauthSignOutRedirectUri = "https://signout"
            }
            urlPath {
                revokeEndpoint = "https://revoke"
                endSessionEndpoint = "https://endsession"
            }
        }
        ConfigHelper.persist(context, frOptions)
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, cookieChanged))
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, realmChanged))
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, clientChanged))
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, expectedURL))
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, redirectURIChanged))
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, signOutRedirectUriChanged))
        assertTrue(ConfigHelper.isConfigDifferentFromPersistedValue(context, scopeChanged))
        assertFalse(ConfigHelper.isConfigDifferentFromPersistedValue(context, frOptions))

        val preference = ConfigHelper.loadFromPreference(context)
        assertEquals(preference, frOptions)

    }

    @Test
    fun loadDefaultValueFromPreference() {
        val expectedValue = FROptionsBuilder.build {
            server {
                url = "https://openam.example.com:8081/openam"
                realm = "root"
                cookieName = "iPlanetDirectoryPro"
            }
            oauth {
                oauthClientId = "andy_app"
                oauthScope = "openid email address"
                oauthRedirectUri = "https://www.example.com:8080/callback"
                oauthSignOutRedirectUri = "https://www.example.com:8080/signout"
            }
            urlPath {
                revokeEndpoint = ""
                endSessionEndpoint = ""
                sessionEndpoint = ""

            }
        }
        val preference = ConfigHelper.loadFromPreference(context)
        assertEquals(preference, expectedValue)
    }

    @Test
    fun closeSession() {
        val config = ConfigHelper.getPersistedConfig(context, null)
        assertNotNull(config)
    }

    @Test
    fun loadDefaultFROptionWithNull() {
       val defaultOption = ConfigHelper.load(context, null)
        assertEquals("https://openam.example.com:8081/openam", defaultOption.server.url)
        assertEquals("root", defaultOption.server.realm)
        assertEquals(30, defaultOption.server.timeout)
        assertEquals("iPlanetDirectoryPro", defaultOption.server.cookieName)
        assertEquals(0, defaultOption.server.cookieCacheSeconds)
        assertEquals("andy_app", defaultOption.oauth.oauthClientId)
        assertEquals("https://www.example.com:8080/callback", defaultOption.oauth.oauthRedirectUri)
        assertEquals("https://www.example.com:8080/signout", defaultOption.oauth.oauthSignOutRedirectUri)
        assertEquals("openid email address", defaultOption.oauth.oauthScope)
        assertEquals(30, defaultOption.oauth.oauthThresholdSeconds)
        assertEquals(0, defaultOption.oauth.oauthCacheSeconds)
        assertEquals("Test", defaultOption.service.authServiceName)
        assertEquals("Registration", defaultOption.service.registrationServiceName)
        assertEquals("", defaultOption.urlPath.authenticateEndpoint)
        assertEquals("", defaultOption.urlPath.revokeEndpoint)
        assertEquals("", defaultOption.urlPath.sessionEndpoint)
        assertEquals("", defaultOption.urlPath.tokenEndpoint)
        assertEquals("", defaultOption.urlPath.userinfoEndpoint)
        assertEquals("", defaultOption.urlPath.authorizeEndpoint)
        assertEquals("", defaultOption.urlPath.endSessionEndpoint)
        assertTrue(defaultOption.sslPinning.buildSteps.isEmpty())
        assertEquals("9hNxmEFgLKGJXqgp61hyb8yIyiT9u0vgDZh4y8TmY/M=", defaultOption.sslPinning.pins[0])
        assertNull(defaultOption.logger.logLevel)
        assertNull(defaultOption.logger.customLogger)
        assertEquals(defaultOption.store.oidcStorage, Options.oidcStorage)
        assertEquals(defaultOption.store.ssoTokenStorage, Options.ssoTokenStorage)
        assertEquals(defaultOption.store.cookiesStorage, Options.cookieStorage)
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
        assertEquals(frOptions, defaultOption)
    }
}