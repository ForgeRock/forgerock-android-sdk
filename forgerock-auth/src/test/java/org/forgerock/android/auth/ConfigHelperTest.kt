package org.forgerock.android.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigHelperTest {

    val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testPersistingData() {

        val frOptions = FROptionsBuilder.build {
            server {
                url = "https://forgerock"
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
        ConfigHelper.persist(context, frOptions)
        val sharedPreferences =
            context.getSharedPreferences(ConfigHelper.ORG_FORGEROCK_V_1_HOSTS, Context.MODE_PRIVATE)
        assertTrue(sharedPreferences.getString("url", null) == "https://forgerock")
        assertTrue(sharedPreferences.getString("realm", null) == "realm123")
        assertTrue(sharedPreferences.getString("cookieName", null) == "cookieName")
        assertTrue(sharedPreferences.getString("client_id", null) == "client_id")
    }

    @Test
    fun testConfigChanged() {
        val frOptions = FROptionsBuilder.build {
            server {
                url = "https://forgerock"
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
        val cookieChanged = FROptionsBuilder.build {
            server {
                cookieName = "cookieName1"
            }
        }
        val realmChanged = FROptionsBuilder.build {
            server {
                realm = "realm1"
            }
        }
        val clientChanged = FROptionsBuilder.build {
            oauth {
               oauthClientId  = "clientId"
            }
        }
        val urlChanged = FROptionsBuilder.build {
            server {
                url  = "url"
            }
        }
        ConfigHelper.persist(context, frOptions)
        assertTrue(ConfigHelper.isConfigChanged(context, cookieChanged))
        assertTrue(ConfigHelper.isConfigChanged(context, realmChanged))
        assertTrue(ConfigHelper.isConfigChanged(context, clientChanged))
        assertTrue(ConfigHelper.isConfigChanged(context, urlChanged))
        assertFalse(ConfigHelper.isConfigChanged(context, frOptions))

        val preference = ConfigHelper().loadFromPreference(context)
        assertTrue(preference == frOptions)
    }

    @Test
    fun loadDefaultFROptionWithNull() {
       val defaultOption = ConfigHelper.load(context, null)
       val expectedResult = "FROptions(server=Server(url=https://openam.example.com:8081/openam, realm=root, timeout=30, cookieName=iPlanetDirectoryPro, oauthUrl=https://openam.example.com:8081/openam), oauth=OAuth(oauthClientId=andy_app, oauthRedirectUri=https://www.example.com:8080/callback, oauthScope=openid email address, oauthThresholdSeconds=30, oauthCacheSeconds=0, cookieCacheSeconds=0), service=Service(authServiceName=Test, registrationServiceName=Registration), urlPath=UrlPath(authenticateEndpoint=, revokeEndpoint=, logoutEndpoint=, tokenEndpoint=, userinfoEndpoint=, authorizeEndpoint=, endSessionEndpoint=), sslPinning=SSLPinning(buildSteps=[], pins=[9hNxmEFgLKGJXqgp61hyb8yIyiT9u0vgDZh4y8TmY/M=]))"
        assertTrue(defaultOption.toString() == expectedResult)
    }

    @Test
    fun loadFROption() {
        val frOptions = FROptionsBuilder.build {
            server {
                url = "https://forgerock"
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
        val expectedResult = "FROptions(server=Server(url=https://forgerock, realm=realm123, timeout=30, cookieName=cookieName, oauthUrl=null), oauth=OAuth(oauthClientId=client_id, oauthRedirectUri=, oauthScope=, oauthThresholdSeconds=30, oauthCacheSeconds=0, cookieCacheSeconds=0), service=Service(authServiceName=null, registrationServiceName=null), urlPath=UrlPath(authenticateEndpoint=null, revokeEndpoint=https://revoke, logoutEndpoint=null, tokenEndpoint=null, userinfoEndpoint=null, authorizeEndpoint=null, endSessionEndpoint=https://endsession), sslPinning=SSLPinning(buildSteps=[], pins=[]))"
        assertTrue(defaultOption.toString() == expectedResult)
    }
}