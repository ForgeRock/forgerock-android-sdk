/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import org.checkerframework.checker.units.qual.C;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import static android.content.Context.MODE_PRIVATE;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class FRAuthMockTest extends BaseTest {

    @Test
    public void frAuthHappyPath() throws InterruptedException, ExecutionException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .context(context)
                .encryptor(new MockEncryptor())
                .build();

        final FRAuth frAuth = FRAuth.builder()
                .serviceName("Example")
                .context(context)
                .sessionManager(SessionManager.builder()
                        .oAuth2Client(oAuth2Client)
                        .tokenManager(new DoNothingTokenManager())
                        .singleSignOnManager(singleSignOnManager)
                        .build())
                .serverConfig(serverConfig)
                .build();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }
            }
        };

        frAuth.next(context, nodeListenerFuture);

        Assert.assertTrue(nodeListenerFuture.get() instanceof AccessToken);

    }

    @Test
    public void frAuthHappyPathWithConfig() throws InterruptedException, ExecutionException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        // Mocking the config
        Resources resources = Mockito.mock(Resources.class);
        when(mockContext.getResources()).thenReturn(resources);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(resources.getInteger(R.integer.forgerock_timeout)).thenReturn(30);

        when(mockContext.getString(R.string.forgerock_url)).thenReturn(getUrl());
        when(mockContext.getString(R.string.forgerock_oauth_url)).thenReturn(getUrl());
        when(mockContext.getString(R.string.forgerock_realm)).thenReturn("root");
        when(mockContext.getString(R.string.forgerock_oauth_redirect_uri)).thenReturn("http://www.example.com:8080/callback");
        when(mockContext.getString(R.string.forgerock_oauth_scope)).thenReturn("openid");
        when(mockContext.getString(R.string.forgerock_oauth_client_id)).thenReturn("andy_app");

        Config.reset();

        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .context(context)
                .encryptor(new MockEncryptor())
                .build();

        final FRAuth frAuth = FRAuth.builder()
                .serviceName("Example")
                .context(mockContext)
                .sessionManager(SessionManager.builder()
                        .oAuth2Client(oAuth2Client)
                        .tokenManager(new DoNothingTokenManager())
                        .singleSignOnManager(singleSignOnManager)
                        .build())
                .serverConfig(serverConfig)
                .build();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(mockContext, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(mockContext, this);
                }
            }
        };

        frAuth.next(mockContext, nodeListenerFuture);

        Assert.assertTrue(nodeListenerFuture.get() instanceof AccessToken);

        RecordedRequest rr = server.takeRequest(); //Start the Auth Service
        assertEquals("/json/realms/root/authenticate?authIndexType=service&authIndexValue=Example", rr.getPath());

        rr = server.takeRequest(); //Post Name Callback
        assertEquals("/json/realms/root/authenticate", rr.getPath());

        rr = server.takeRequest(); //Post Password Callback
        assertEquals("/json/realms/root/authenticate", rr.getPath());

        rr = server.takeRequest(); //Post to /authorize endpoint
        assertTrue(rr.getPath().startsWith("/oauth2/realms/root/authorize?iPlanetDirectoryPro="));

        rr = server.takeRequest(); //Post to /access-token endpoint
        assertEquals("/oauth2/realms/root/access_token", rr.getPath());

    }

    @Test
    public void testAccessTokenAndSSODisabled() throws ExecutionException, InterruptedException, AuthenticationRequiredException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        final TokenManager tokenManager = DefaultTokenManager.builder()
                .context(context)
                .oAuth2Client(oAuth2Client)
                .sharedPreferences(context.getSharedPreferences("Test", Context.MODE_PRIVATE))
                .build();

        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .context(context)
                .disableSSO(true)
                .build();

        final FRAuth frAuth = FRAuth.builder()
                .serviceName("Example")
                .context(context)
                .sessionManager(SessionManager.builder()
                        .oAuth2Client(oAuth2Client)
                        .tokenManager(tokenManager)
                        .singleSignOnManager(singleSignOnManager)
                        .build())
                .serverConfig(serverConfig)
                .build();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }
            }
        };

        frAuth.next(context, nodeListenerFuture);

        Assert.assertTrue(nodeListenerFuture.get() instanceof AccessToken);

        //Check AccessToken Storage
        AccessToken accessToken = tokenManager.getAccessToken();
        assertNotNull(accessToken);
        assertNotNull(accessToken.getValue());

        //Check SSOToken Storage
        Token token = singleSignOnManager.getToken();
        assertNull(token);
    }

    @Test
    public void testAccessTokenAndSSOToken() throws ExecutionException, InterruptedException, AuthenticationRequiredException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        final TokenManager tokenManager = DefaultTokenManager.builder()
                .context(context)
                .oAuth2Client(oAuth2Client)
                .sharedPreferences(context.getSharedPreferences("Test", Context.MODE_PRIVATE))
                .build();

        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .context(context)
                .encryptor(new MockEncryptor())
                .build();

        final FRAuth frAuth = FRAuth.builder()
                .serviceName("Example")
                .context(context)
                .sessionManager(SessionManager.builder()
                        .oAuth2Client(oAuth2Client)
                        .tokenManager(tokenManager)
                        .singleSignOnManager(singleSignOnManager)
                        .build())
                .serverConfig(serverConfig)
                .build();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }
            }
        };

        frAuth.next(context, nodeListenerFuture);

        Assert.assertTrue(nodeListenerFuture.get() instanceof AccessToken);

        //Check AccessToken Storage
        AccessToken accessToken = tokenManager.getAccessToken();
        assertNotNull(accessToken);
        assertNotNull(accessToken.getValue());

        //Check SSOToken Storage
        Token token = singleSignOnManager.getToken();
        assertNotNull(token);
        assertNotNull(token.getValue());
    }

    @Test
    public void testAccessTokenAndSSOTokenRefreshWithSSOToken() throws ExecutionException, InterruptedException, AuthenticationRequiredException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        final TokenManager tokenManager = DefaultTokenManager.builder()
                .context(context)
                .oAuth2Client(oAuth2Client)
                .sharedPreferences(context.getSharedPreferences("Test", Context.MODE_PRIVATE))
                .build();

        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .context(context)
                .encryptor(new MockEncryptor())
                .build();

        SessionManager sessionManager = SessionManager.builder()
                .tokenManager(tokenManager)
                .singleSignOnManager(singleSignOnManager)
                .oAuth2Client(oAuth2Client)
                .build();


        final FRAuth frAuth = FRAuth.builder()
                .serviceName("Example")
                .context(context)
                .sessionManager(sessionManager)
                .serverConfig(serverConfig)
                .build();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }
            }
        };

        frAuth.next(context, nodeListenerFuture);

        Assert.assertTrue(nodeListenerFuture.get() instanceof AccessToken);

        //Check AccessToken Storage
        AccessToken accessToken = tokenManager.getAccessToken();
        assertNotNull(accessToken);
        assertNotNull(accessToken.getValue());

        //Check SSOToken Storage
        Token token = singleSignOnManager.getToken();
        assertNotNull(token);
        assertNotNull(token.getValue());

        tokenManager.clear();

        accessToken = sessionManager.getAccessToken();
        assertNotNull(accessToken.getValue());


    }

    @Test
    public void testSSOEnabled() throws ExecutionException, InterruptedException, AuthenticationRequiredException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        //For AppB
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        final TokenManager tokenManager = DefaultTokenManager.builder()
                .context(context)
                .oAuth2Client(oAuth2Client)
                .sharedPreferences(context.getSharedPreferences("Test", Context.MODE_PRIVATE))
                .build();

        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .context(context)
                .encryptor(new MockEncryptor())
                .build();

        final FRAuth frAuth = FRAuth.builder()
                .serviceName("Example")
                .context(context)
                .sessionManager(SessionManager.builder()
                        .oAuth2Client(oAuth2Client)
                        .tokenManager(tokenManager)
                        .singleSignOnManager(singleSignOnManager)
                        .build())
                .serverConfig(serverConfig)
                .build();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }
            }
        };

        frAuth.next(context, nodeListenerFuture);

        Assert.assertTrue(nodeListenerFuture.get() instanceof AccessToken);

        RecordedRequest rr = server.takeRequest(); //Start the Auth Service
        rr = server.takeRequest(); //Post Name Callback
        rr = server.takeRequest(); //Post Password Callback
        rr = server.takeRequest(); //Post to /authorize endpoint
        rr = server.takeRequest(); //Post to /access-token endpoint

        //Switch to another App with Access Token does not exists
        tokenManager.clear();

        final boolean[] callbackReceived = new boolean[1];
        NodeListenerFuture appB = new NodeListenerFuture() {
            @Override
            public void onCallbackReceived(Node node) {
                callbackReceived[0] = true;
            }
        };
        frAuth.next(context, appB);
        assertFalse(callbackReceived[0]); //Should not receive callback
        Assert.assertTrue(appB.get() instanceof AccessToken);
        rr = server.takeRequest(); //Post to /authorize endpoint
        rr = server.takeRequest(); //Post to /access-token endpoint

        //Check AccessToken Storage
        AccessToken accessToken = tokenManager.getAccessToken();
        assertNotNull(accessToken);
        assertNotNull(accessToken.getValue());

        //Check SSOToken Storage
        Token token = singleSignOnManager.getToken();
        assertNotNull(token);
        assertNotNull(token.getValue());

    }

    @Test
    public void startTest() {

        SharedPreferences sharedPreferences = context.getSharedPreferences(FRAuth.ORG_FORGEROCK_V_1_HOSTS, MODE_PRIVATE);
        sharedPreferences.edit().putString("url", "something").commit();

        FRAuth.start(context);
        //host url is created
        sharedPreferences = context.getSharedPreferences(FRAuth.ORG_FORGEROCK_V_1_HOSTS, MODE_PRIVATE);
        assertEquals(Config.getInstance().getUrl(), sharedPreferences.getString("url", null));

    }
}
