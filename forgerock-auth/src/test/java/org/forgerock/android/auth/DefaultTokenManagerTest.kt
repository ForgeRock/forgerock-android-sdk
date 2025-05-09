/*
 * Copyright (c) 2019 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.mockwebserver.MockResponse
import org.forgerock.android.auth.exception.AuthenticationRequiredException
import org.forgerock.android.auth.exception.InvalidGrantException
import org.forgerock.android.auth.storage.Storage
import org.forgerock.android.auth.storage.StorageDelegate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.util.concurrent.ExecutionException

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class DefaultTokenManagerTest : BaseTest() {

    private lateinit var storage: Storage<AccessToken>

    @Before
    fun setUpStorage() {
        //Cannot use MemoryStorage as it does not support cacheable
        storage = sharedPreferencesStorage<AccessToken>(context = context,
            filename = DEFAULT_TOKEN_MANAGER_TEST,
            key = "AccessToken", cacheable = false)
        Options.init(Store(oidcStorage = storage))
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        storage.delete()
        Options.init(Store())
    }

    @Test
    @Throws(Throwable::class)
    fun storeAccessToken() {
        val tokenManager: TokenManager = DefaultTokenManager.builder()
            .context(context).build()
        val accessToken = AccessToken.builder()
            .value("access token")
            .idToken("id token")
            .scope(AccessToken.Scope.parse("openid test"))
            .tokenType("Bearer")
            .refreshToken("refresh token")
            .expiresIn(100)
            .build()
        tokenManager.persist(accessToken)
        val storedAccessToken = getAccessToken(tokenManager)
        assertEquals("access token", storedAccessToken.value)
        assertEquals("id token", storedAccessToken.idToken)
        assertTrue(storedAccessToken.scope!!.contains("openid"))
        assertTrue(storedAccessToken.scope!!.contains("test"))
        assertEquals("Bearer", storedAccessToken.tokenType)
        assertEquals("refresh token", storedAccessToken.refreshToken)
    }

    @Test(expected = AuthenticationRequiredException::class)
    @Throws(Throwable::class)
    fun clearAccessToken() {
        val tokenManager: TokenManager = DefaultTokenManager.builder()
            .context(context).build()
        val accessToken = AccessToken.builder()
            .value("access token")
            .idToken("id token")
            .scope(AccessToken.Scope.parse("openid test"))
            .tokenType("Bearer")
            .refreshToken("refresh token")
            .expiresIn(100)
            .build()
        tokenManager.persist(accessToken)
        tokenManager.clear()
        getAccessToken(tokenManager)
    }

    @Test
    @Throws(Throwable::class)
    fun tokenManagerWithCache() {

        val tokenManager: TokenManager = DefaultTokenManager.builder()
            .cacheIntervalMillis(100L)
            .context(context).build()
        val accessToken = AccessToken.builder()
            .value("access token")
            .idToken("id token")
            .scope(AccessToken.Scope.parse("openid test"))
            .tokenType("Bearer")
            .refreshToken("refresh token")
            .expiresIn(300)
            .build()
        tokenManager.persist(accessToken)
        val storedAccessToken1 = getAccessToken(tokenManager)
        val storedAccessToken2 = getAccessToken(tokenManager)

        //If reference are equal, they come from the cache
        assertSame(storedAccessToken1, storedAccessToken2)

        Thread.sleep(200)

        val storedAccessToken3 = getAccessToken(tokenManager)
        //The cache is expired, should re-cache and token should not have the same references
        assertNotSame(storedAccessToken1, storedAccessToken3)

        //Confirm that the token is re-cached
        val storedAccessToken4 = getAccessToken(tokenManager)
        assertSame(storedAccessToken3, storedAccessToken4)

    }

    @Test
    @Throws(Throwable::class)
    fun tokenManagerWithoutCache() {

        val tokenManager: TokenManager = DefaultTokenManager.builder()
            .context(context).build()
        val accessToken = AccessToken.builder()
            .value("access token")
            .idToken("id token")
            .scope(AccessToken.Scope.parse("openid test"))
            .tokenType("Bearer")
            .refreshToken("refresh token")
            .expiresIn(100)
            .build()
        tokenManager.persist(accessToken)
        val storedAccessToken1 = getAccessToken(tokenManager)
        val storedAccessToken2 = getAccessToken(tokenManager)

        //If reference are equal, they come from the cache
        assertNotSame(storedAccessToken1, storedAccessToken2)
    }

    @Test
    @Throws(Throwable::class)
    fun testCacheExpired() {
        val tokenManager: TokenManager = DefaultTokenManager.builder()
            .cacheIntervalMillis(100L)
            .context(context).build()
        val accessToken = AccessToken.builder()
            .value("access token")
            .idToken("id token")
            .scope(AccessToken.Scope.parse("openid test"))
            .tokenType("Bearer")
            .refreshToken("refresh token")
            .expiresIn(100)
            .build()
        tokenManager.persist(accessToken)
        val storedAccessToken1 = getAccessToken(tokenManager)

        Thread.sleep(200L)

        val storedAccessToken2 = getAccessToken(tokenManager)

        //If reference are equal, they come from the cache
        assertNotSame(storedAccessToken1, storedAccessToken2)

    }

    @Test
    @Throws(Throwable::class)
    fun testTokenRefresh() {

        enqueue("/authenticate_refreshToken.json", HttpURLConnection.HTTP_OK)
        val tokenManager: TokenManager = DefaultTokenManager.builder()
            .oAuth2Client(oAuth2Client)
            .threshold(0L)
            .context(context).build()
        val accessToken = AccessToken.builder()
            .value("access token")
            .idToken("id token")
            .scope(AccessToken.Scope.parse("openid test"))
            .tokenType("Bearer")
            .refreshToken("refresh token")
            .expiresIn(1)
            .sessionToken(SSOToken("dummy"))
            .build()
        tokenManager.persist(accessToken)
        val storedAccessToken1 = getAccessToken(tokenManager)

        Thread.sleep(1000)
        val storedAccessToken2 = getAccessToken(tokenManager)
        assertNotEquals(storedAccessToken1.value, storedAccessToken2.value)
        assertEquals("Refreshed Token", storedAccessToken2.value)
    }

    @Test
    @Throws(Throwable::class)
    fun testTokenRefreshWithNotIssueNewRefreshToken() {
        enqueue("/authenticate_without_refreshToken.json", HttpURLConnection.HTTP_OK)
        val tokenManager: TokenManager = DefaultTokenManager.builder()
            .oAuth2Client(oAuth2Client)
            .threshold(0L)
            .context(context).build()

        //Existing access token with refresh token
        val accessToken = AccessToken.builder()
            .value("access token")
            .idToken("id token")
            .scope(AccessToken.Scope.parse("openid test"))
            .tokenType("Bearer")
            .refreshToken("refresh token")
            .expiresIn(1)
            .sessionToken(SSOToken("dummy"))
            .build()
        tokenManager.persist(accessToken)
        val storedAccessToken1 = getAccessToken(tokenManager)
        Thread.sleep(1000)

        val storedAccessToken2 = getAccessToken(tokenManager)
        assertNotEquals(storedAccessToken1.value, storedAccessToken2.value)
        assertEquals("Refreshed Token", storedAccessToken2.value)
        assertEquals("refresh token", storedAccessToken2.refreshToken)
    }

    @Test
    @Throws(Throwable::class)
    fun testTokenRefreshWithThreshold() {
        //asyn revoke call
        enqueue("/authenticate_refreshToken.json", HttpURLConnection.HTTP_OK)
        enqueue("/authenticate_refreshToken.json", HttpURLConnection.HTTP_OK)
        val oAuth2Client = OAuth2Client.builder()
            .clientId("andy_app")
            .scope("openid email address")
            .redirectUri("http://www.example.com:8080/callback")
            .serverConfig(serverConfig)
            .build()
        val tokenManager: TokenManager = DefaultTokenManager.builder()
            .oAuth2Client(oAuth2Client)
            .threshold(1L)
            .context(context).build()
        val accessToken = AccessToken.builder()
            .value("access token")
            .idToken("id token")
            .scope(AccessToken.Scope.parse("openid test"))
            .tokenType("Bearer")
            .refreshToken("refresh token")
            .expiresIn(1) //expire in 1 seconds
            .sessionToken(SSOToken("dummy"))
            .build()
        tokenManager.persist(accessToken)
        val storedAccessToken2 = getAccessToken(tokenManager) //set threshold for 1 second
        assertNotEquals(accessToken.value, storedAccessToken2.value)

    }

    @Test(expected = InvalidGrantException::class)
    @Throws(Throwable::class)
    fun testTokenRefreshWithException() {
        server.enqueue(
            MockResponse()
                .setBody(
                    """{
    "error_description": "grant is invalid",
    "error": "invalid_grant"}"""
                )
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
        )
        val tokenManager: TokenManager = DefaultTokenManager.builder()
            .oAuth2Client(oAuth2Client)
            .threshold(0L)
            .context(context).build()
        val accessToken = AccessToken.builder()
            .value("access token")
            .idToken("id token")
            .scope(AccessToken.Scope.parse("openid test"))
            .tokenType("Bearer")
            .refreshToken("refresh token")
            .expiresIn(1)
            .sessionToken(SSOToken("dummy"))
            .build()
        tokenManager.persist(accessToken)
        val storedAccessToken1 = getAccessToken(tokenManager)
        assertNotNull(storedAccessToken1)
        Thread.sleep(1000)
        getAccessToken(tokenManager)
    }

    @Throws(Throwable::class)
    private fun getAccessToken(tokenManager: TokenManager): AccessToken {
        val future = FRListenerFuture<AccessToken>()
        tokenManager.getAccessToken(null, future)
        return try {
            future.get()
        } catch (e: ExecutionException) {
            throw e.cause!!
        } catch (e: InterruptedException) {
            throw e
        }
    }

    companion object {
        const val DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest"
    }

}

class SharedPreferencesStorage<T : @Serializable Any>(
    context: Context,
    filename: String,
    private var key: String,
    private val serializer: KSerializer<T>,
) : Storage<T> {

    private var sharedPreferences: SharedPreferences = context.getSharedPreferences(
        filename,
        Context.MODE_PRIVATE
    )

    override fun save(item: T) {
        val s = json.encodeToString(serializer, item)
        sharedPreferences.edit().putString(key, s).commit()
    }

    override fun get(): T? {
        return sharedPreferences.getString(key, null)?.let {
            val s = json.decodeFromString(serializer, it)
            return s
        }
    }

    override fun delete() {
        sharedPreferences.edit().remove(key).commit()
    }
}

inline fun <reified T : @Serializable Any> sharedPreferencesStorage(
    context: Context,
    filename: String,
    key: String,
    cacheable: Boolean = false,
): StorageDelegate<T> =
    StorageDelegate {
        SharedPreferencesStorage(
            context = context,
            filename = filename,
            key = key,
            serializer = Json.serializersModule.serializer(),
        )
    }

