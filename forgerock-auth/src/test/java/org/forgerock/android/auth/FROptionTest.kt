package org.forgerock.android.auth

import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test

class FROptionTest {
    @Test
    fun testDefaultBuilderOption() {
        val option = FROptionsBuilder.build { }
        assertTrue(option.sslPinning.pins == emptyList<String>())
        assertTrue(option.sslPinning.buildSteps == emptyList<BuildStep<OkHttpClient.Builder>>())
        assertTrue(option.server.realm == "root")
        assertTrue(option.server.url == "")
        assertTrue(option.server.cookieName == "iPlanetDirectoryPro")
        assertTrue(option.server.oauthUrl == null)
        assertTrue(option.server.timeout == 30)
        assertTrue(option.oauth.oauthClientId == "")
        assertTrue(option.oauth.oauthRedirectUri == "")
        assertTrue(option.oauth.oauthScope == "")
        assertTrue(option.oauth.oauthThresholdSeconds == 30.toLong())
        assertTrue(option.oauth.oauthCacheSeconds == 0.toLong())
        assertTrue(option.oauth.cookieCacheSeconds == 0.toLong())
        assertTrue(option.service.authServiceName == null)
        assertTrue(option.service.registrationServiceName == null)
        assertTrue(option.urlPath.authorizeEndpoint == null)
        assertTrue(option.urlPath.revokeEndpoint == null)
        assertTrue(option.urlPath.logoutEndpoint == null)
        assertTrue(option.urlPath.tokenEndpoint == null)
        assertTrue(option.urlPath.userinfoEndpoint == null)
        assertTrue(option.urlPath.authorizeEndpoint == null)
        assertTrue(option.urlPath.endSessionEndpoint == null)
    }
}