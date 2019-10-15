/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.squareup.okhttp.mockwebserver.MockResponse;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.HttpURLConnection;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(RobolectricTestRunner.class)
public class DefaultTokenManagerTest extends BaseTest {

    private static final String DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest";
    private Context context = ApplicationProvider.getApplicationContext();

    @After
    public void tearDown() throws Exception {
        context.deleteSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST);
    }

    @Test
    public void storeAccessToken() throws AuthenticationRequiredException {

        TokenManager tokenManager = DefaultTokenManager.builder()
                .sharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE))
                .context(context).build();

        AccessToken accessToken = AccessToken.builder()
                .value("access token")
                .idToken("id token")
                .scope(AccessToken.Scope.parse("openid test"))
                .tokenType("Bearer")
                .refreshToken("refresh token")
                .expiresIn(100)
                .build();

        tokenManager.persist(accessToken);

        AccessToken storedAccessToken = tokenManager.getAccessToken();
        assertEquals("access token", storedAccessToken.getValue());
        assertEquals("id token", storedAccessToken.getIdToken());
        assertTrue(storedAccessToken.getScope().contains("openid"));
        assertTrue(storedAccessToken.getScope().contains("test"));
        assertEquals("Bearer", storedAccessToken.getTokenType());
        assertEquals("refresh token", storedAccessToken.getRefreshToken());

    }

    @Test(expected = AuthenticationRequiredException.class)
    public void clearAccessToken() throws AuthenticationRequiredException {

        TokenManager tokenManager = DefaultTokenManager.builder()
                .sharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE))
                .context(context).build();

        AccessToken accessToken = AccessToken.builder()
                .value("access token")
                .idToken("id token")
                .scope(AccessToken.Scope.parse("openid test"))
                .tokenType("Bearer")
                .refreshToken("refresh token")
                .expiresIn(100)
                .build();

        tokenManager.persist(accessToken);
        tokenManager.clear();

        tokenManager.getAccessToken();
    }

    @Test
    public void tokenManagerWithCache() throws AuthenticationRequiredException {

        TokenManager tokenManager = DefaultTokenManager.builder()
                .sharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE))
                .cacheIntervalMillis(100L)
                .context(context).build();

        AccessToken accessToken = AccessToken.builder()
                .value("access token")
                .idToken("id token")
                .scope(AccessToken.Scope.parse("openid test"))
                .tokenType("Bearer")
                .refreshToken("refresh token")
                .expiresIn(100)
                .build();

        tokenManager.persist(accessToken);

        AccessToken storedAccessToken1 = tokenManager.getAccessToken();
        AccessToken storedAccessToken2 = tokenManager.getAccessToken();

        //If reference are equal, they come from the cache
        assertSame(storedAccessToken1, storedAccessToken2);
    }

    @Test
    public void tokenManagerWithoutCache() throws AuthenticationRequiredException {

        TokenManager tokenManager = DefaultTokenManager.builder()
                .sharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE))
                .context(context).build();

        AccessToken accessToken = AccessToken.builder()
                .value("access token")
                .idToken("id token")
                .scope(AccessToken.Scope.parse("openid test"))
                .tokenType("Bearer")
                .refreshToken("refresh token")
                .expiresIn(100)
                .build();

        tokenManager.persist(accessToken);

        AccessToken storedAccessToken1 = tokenManager.getAccessToken();
        AccessToken storedAccessToken2 = tokenManager.getAccessToken();

        //If reference are equal, they come from the cache
        assertNotSame(storedAccessToken1, storedAccessToken2);

    }

    @Test
    public void testCacheExpired() throws AuthenticationRequiredException, InterruptedException {

        TokenManager tokenManager = DefaultTokenManager.builder()
                .sharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE))
                .cacheIntervalMillis(100L)
                .context(context).build();

        AccessToken accessToken = AccessToken.builder()
                .value("access token")
                .idToken("id token")
                .scope(AccessToken.Scope.parse("openid test"))
                .tokenType("Bearer")
                .refreshToken("refresh token")
                .expiresIn(100)
                .build();

        tokenManager.persist(accessToken);

        AccessToken storedAccessToken1 = tokenManager.getAccessToken();
        Thread.sleep(200);
        AccessToken storedAccessToken2 = tokenManager.getAccessToken();

        //If reference are equal, they come from the cache
        assertNotSame(storedAccessToken1, storedAccessToken2);
    }

    @Test
    public void testTokenRefresh() throws AuthenticationRequiredException, InterruptedException {

        enqueue("/authenticate_refreshToken.json", HttpURLConnection.HTTP_OK);

        TokenManager tokenManager = DefaultTokenManager.builder()
                .sharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE))
                .oAuth2Client(oAuth2Client)
                .threshold(0L)
                .context(context).build();

        AccessToken accessToken = AccessToken.builder()
                .value("access token")
                .idToken("id token")
                .scope(AccessToken.Scope.parse("openid test"))
                .tokenType("Bearer")
                .refreshToken("refresh token")
                .expiresIn(1)
                .build();

        tokenManager.persist(accessToken);

        AccessToken storedAccessToken1 = tokenManager.getAccessToken();
        Thread.sleep(1000);
        AccessToken storedAccessToken2 = tokenManager.getAccessToken();

        assertNotEquals(storedAccessToken1.getValue(), storedAccessToken2.getValue());
        assertEquals("Refreshed Token", storedAccessToken2.getValue());
    }

    @Test
    public void testTokenRefreshWithThreshold() throws AuthenticationRequiredException, InterruptedException {

        enqueue("/authenticate_refreshToken.json", HttpURLConnection.HTTP_OK);

        final OAuth2Client oAuth2Client = OAuth2Client.builder()
                .clientId("andy_app")
                .scope("openid email address")
                .redirectUri("http://www.example.com:8080/callback")
                .serverConfig(serverConfig)
                .build();

        TokenManager tokenManager = DefaultTokenManager.builder()
                .sharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE))
                .oAuth2Client(oAuth2Client)
                .threshold(1L)
                .context(context).build();

        AccessToken accessToken = AccessToken.builder()
                .value("access token")
                .idToken("id token")
                .scope(AccessToken.Scope.parse("openid test"))
                .tokenType("Bearer")
                .refreshToken("refresh token")
                .expiresIn(1) //expire in 1 seconds
                .build();

        tokenManager.persist(accessToken);

        AccessToken storedAccessToken1 = tokenManager.getAccessToken();
        Thread.sleep(2000); //sleep for 2 seconds
        AccessToken storedAccessToken2 = tokenManager.getAccessToken(); //set threshold for 1 second

        assertNotEquals(storedAccessToken1.getValue(), storedAccessToken2.getValue());
    }

    @Test(expected = AuthenticationRequiredException.class)
    public void testTokenRefreshWithException() throws AuthenticationRequiredException, InterruptedException {

        server.enqueue(new MockResponse()
                .setBody("{\n" +
                        "    \"error_description\": \"grant is invalid\",\n" +
                        "    \"error\": \"invalid_grant\"\n" +
                        "}")
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST));

        TokenManager tokenManager = DefaultTokenManager.builder()
                .sharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE))
                .oAuth2Client(oAuth2Client)
                .threshold(0L)
                .context(context).build();

        AccessToken accessToken = AccessToken.builder()
                .value("access token")
                .idToken("id token")
                .scope(AccessToken.Scope.parse("openid test"))
                .tokenType("Bearer")
                .refreshToken("refresh token")
                .expiresIn(1)
                .build();

        tokenManager.persist(accessToken);

        AccessToken storedAccessToken1 = tokenManager.getAccessToken();
        assertNotNull(storedAccessToken1);
        Thread.sleep(1000);
        tokenManager.getAccessToken();

    }



}
