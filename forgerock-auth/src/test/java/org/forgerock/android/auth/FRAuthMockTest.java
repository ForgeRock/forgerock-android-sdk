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

import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class FRAuthMockTest extends BaseTest {

    @Test
    public void frAuthHappyPath() throws InterruptedException, ExecutionException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .serverConfig(serverConfig)
                .context(context)
                .encryptor(new MockEncryptor())
                .build();

        final FRAuth frAuth = FRAuth.builder()
                .serviceName("Example")
                .context(context)
                .sessionManager(SessionManager.builder()
                        .tokenManager(Config.getInstance().getTokenManager())
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

        Assert.assertTrue(nodeListenerFuture.get() instanceof SSOToken);

    }

    @Test
    public void frAuthHappyPathWithConfig() throws InterruptedException, ExecutionException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

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
        Config.getInstance().init(context);

        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .context(context)
                .encryptor(new MockEncryptor())
                .build();

        final FRAuth frAuth = FRAuth.builder()
                .serviceName("Example")
                .context(mockContext)
                .sessionManager(SessionManager.builder()
                        .tokenManager(Config.getInstance().getTokenManager())
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

        Assert.assertTrue(nodeListenerFuture.get() instanceof SSOToken);

        RecordedRequest rr = server.takeRequest(); //Start the Auth Service
        assertEquals("/json/realms/root/authenticate?authIndexType=service&authIndexValue=Example", rr.getPath());

        rr = server.takeRequest(); //Post Name Callback
        assertEquals("/json/realms/root/authenticate", rr.getPath());

        rr = server.takeRequest(); //Post Password Callback
        assertEquals("/json/realms/root/authenticate", rr.getPath());

    }

    @Test
    public void testWithSharedPreferencesSSOManager() throws ExecutionException, InterruptedException, AuthenticationRequiredException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        final SingleSignOnManager singleSignOnManager = SharedPreferencesSignOnManager.builder()
                .context(context)
                .sharedPreferences(context.getSharedPreferences("Test", Context.MODE_PRIVATE))
                .build();

        final FRAuth frAuth = FRAuth.builder()
                .serviceName("Example")
                .context(context)
                .sessionManager(SessionManager.builder()
                        .tokenManager(Config.getInstance().getTokenManager())
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

        Assert.assertTrue(nodeListenerFuture.get() instanceof SSOToken);

        //Check SSOToken Storage
        Token token = singleSignOnManager.getToken();
        assertNotNull(token);
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

    @Test
    public void testStart() throws ExecutionException, InterruptedException {
        //Trigger the first request
        frAuthHappyPath();

        final int[] count = {0};

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().endsWith("/sessions?_action=logout")) {
                    return response("/sessions_logout.json", HttpURLConnection.HTTP_OK);
                }

                if (count[0] == 0) {
                    count[0]++;
                    return response("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
                }
                if (count[0] == 1) {
                    count[0]++;
                    return response("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
                }
                if (count[0] == 2) {
                    count[0]++;
                    return response("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
                }
                throw new IllegalArgumentException("Not expected Request");
            }
        });

        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .serverConfig(serverConfig)
                .context(context)
                .encryptor(new MockEncryptor())
                .build();

        final FRAuth frAuth = FRAuth.builder()
                .serviceName("Example")
                .context(context)
                .sessionManager(SessionManager.builder()
                        .tokenManager(Config.getInstance().getTokenManager())
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

        frAuth.start(context, nodeListenerFuture);

        RecordedRequest recordedRequest = server.takeRequest(); //username
        recordedRequest = server.takeRequest(); //password
        recordedRequest = server.takeRequest(); //session
        //calling start
        recordedRequest = server.takeRequest(); //logout
        recordedRequest = server.takeRequest(); //username
        recordedRequest = server.takeRequest(); //password
        recordedRequest = server.takeRequest(); //session

        Assert.assertTrue(nodeListenerFuture.get() instanceof SSOToken);



    }
}
