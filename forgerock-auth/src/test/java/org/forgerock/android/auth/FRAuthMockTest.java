/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static android.content.Context.MODE_PRIVATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class FRAuthMockTest extends BaseTest {

    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() throws Exception {
        sharedPreferences = context.getSharedPreferences("Dummy", MODE_PRIVATE);
    }

    @After
    public void tearDown() throws Exception {
        sharedPreferences.edit().clear().commit();
    }

    @Test
    public void frAuthHappyPath() throws InterruptedException, ExecutionException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .sharedPreferences(sharedPreferences)
                .serverConfig(serverConfig)
                .context(context)
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
        Config.getInstance().init(context, null);

        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .sharedPreferences(sharedPreferences)
                .context(context)
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

        SharedPreferences sharedPreferences = context.getSharedPreferences(ConfigHelper.ORG_FORGEROCK_V_1_HOSTS, MODE_PRIVATE);
        sharedPreferences.edit().putString("url", "https://somethingelse").commit();

        FRAuth.start(context);
        //host url is created

        assertEquals(Config.getInstance().getUrl(), sharedPreferences.getString("url", null));
        assertEquals(Config.getInstance().getRealm(), sharedPreferences.getString("realm", null));
        assertEquals(Config.getInstance().getCookieName(), sharedPreferences.getString("cookieName", null));
        assertEquals(Config.getInstance().getClientId(), sharedPreferences.getString("client_id", null));
        assertEquals(Config.getInstance().getScope(), sharedPreferences.getString("scope", null));
        assertEquals(Config.getInstance().getRedirectUri(), sharedPreferences.getString("redirect_uri", null));

        FRAuth.start(context, null);
        //host url is created

        assertEquals(Config.getInstance().getUrl(), sharedPreferences.getString("url", null));
        assertEquals(Config.getInstance().getRealm(), sharedPreferences.getString("realm", null));
        assertEquals(Config.getInstance().getCookieName(), sharedPreferences.getString("cookieName", null));
        assertEquals(Config.getInstance().getClientId(), sharedPreferences.getString("client_id", null));
        assertEquals(Config.getInstance().getScope(), sharedPreferences.getString("scope", null));
        assertEquals(Config.getInstance().getRedirectUri(), sharedPreferences.getString("redirect_uri", null));

    }

    @Test
    public void startTestWithOptionsAndNull() {

        SharedPreferences sharedPreferences = context.getSharedPreferences(ConfigHelper.ORG_FORGEROCK_V_1_HOSTS, MODE_PRIVATE);
        sharedPreferences.edit().putString("url", "https://somethingelseone").commit();

        FROptions frOptions = FROptionsBuilder.build(frOptionsBuilder -> {
            frOptionsBuilder.server(serverBuilder -> {
                serverBuilder.setUrl("https://forgerocker.com");
                serverBuilder.setRealm("realm");
                serverBuilder.setCookieName("planet");
                serverBuilder.setCookieCacheSeconds(5000);
                serverBuilder.setTimeout(50);
                return null;
            });
            frOptionsBuilder.oauth(oAuthBuilder -> {
                oAuthBuilder.setOauthClientId("client_id");
                oAuthBuilder.setOauthRedirectUri("https://redirecturi");
                oAuthBuilder.setOauthScope("scope");
                oAuthBuilder.setOauthThresholdSeconds(5000);
                return null;
            });
            frOptionsBuilder.urlPath(urlPathBuilder -> {
                urlPathBuilder.setRevokeEndpoint("https://revokeEndpoint.com");
                urlPathBuilder.setEndSessionEndpoint("https://endSessionEndpoint.com");
                urlPathBuilder.setAuthenticateEndpoint("https://authenticateEndpoint.com");
                urlPathBuilder.setAuthorizeEndpoint("https://authorizeEndpoint.com");
                urlPathBuilder.setSessionEndpoint("https://logoutEndpoint.com");
                urlPathBuilder.setTokenEndpoint("https://tokenEndpoint.com");
                return null;
            });
            frOptionsBuilder.sslPinning(sslPinningBuilder -> {
                sslPinningBuilder.setBuildSteps(Collections.emptyList());
                sslPinningBuilder.setPins(Collections.emptyList());
                return null;
            });
            frOptionsBuilder.service(serviceBuilder -> {
                serviceBuilder.setAuthServiceName("auth_service");
                serviceBuilder.setRegistrationServiceName("reg_service");
                return null;
            });
            return null;
        });

        FRAuth.start(context, frOptions);
        //host url is created
        assertEquals(Config.getInstance().getUrl(), sharedPreferences.getString("url", null));
        assertEquals(Config.getInstance().getRealm(), sharedPreferences.getString("realm", null));
        assertEquals(Config.getInstance().getCookieName(), sharedPreferences.getString("cookieName", null));
        assertEquals(Config.getInstance().getClientId(), sharedPreferences.getString("client_id", null));
        assertEquals(Config.getInstance().getScope(), sharedPreferences.getString("scope", null));
        assertEquals(Config.getInstance().getRedirectUri(), sharedPreferences.getString("redirect_uri", null));

        assertEquals(Config.getInstance().getPins(), frOptions.getSslPinning().getPins());
        assertEquals(Config.getInstance().getBuildSteps(), frOptions.getSslPinning().getBuildSteps());
        assertEquals(Config.getInstance().getAuthServiceName(), frOptions.getService().getAuthServiceName());
        assertEquals(Config.getInstance().getRegistrationServiceName(), frOptions.getService().getRegistrationServiceName());

        assertEquals(Config.getInstance().getCookieName(), frOptions.getServer().getCookieName());
        Long millisCookieCache = frOptions.getServer().getCookieCacheSeconds() * 1000;
        assertEquals(Config.getInstance().getCookieCacheMillis(), millisCookieCache);

        assertEquals(Config.getInstance().getRealm(), frOptions.getServer().getRealm());
        assertEquals(Config.getInstance().getTimeout(), frOptions.getServer().getTimeout());
        assertEquals(Config.getInstance().getUrl(), frOptions.getServer().getUrl());

        assertEquals(Config.getInstance().getRedirectUri(), frOptions.getOauth().getOauthRedirectUri());
        assertEquals(Config.getInstance().getScope(), frOptions.getOauth().getOauthScope());
        assertEquals(Config.getInstance().getClientId(), frOptions.getOauth().getOauthClientId());
        Long millisOauthCache = frOptions.getOauth().getOauthCacheSeconds() * 1000;
        assertEquals(Config.getInstance().getOauthCacheMillis(), millisOauthCache);
        Long thresholdSeconds = frOptions.getOauth().getOauthThresholdSeconds();
        assertEquals(Config.getInstance().getOauthThreshold(), thresholdSeconds);

        FRAuth.start(context, null);
        SharedPreferences sharedPreferences1 = context.getSharedPreferences(ConfigHelper.ORG_FORGEROCK_V_1_HOSTS, MODE_PRIVATE);
        //host url is created
        assertEquals(Config.getInstance().getUrl(), sharedPreferences1.getString("url", null));
        assertEquals(Config.getInstance().getRealm(), sharedPreferences1.getString("realm", null));
        assertEquals(Config.getInstance().getCookieName(), sharedPreferences1.getString("cookieName", null));
        assertEquals(Config.getInstance().getClientId(), sharedPreferences1.getString("client_id", null));
        assertEquals(Config.getInstance().getScope(), sharedPreferences1.getString("scope", null));
        assertEquals(Config.getInstance().getRedirectUri(), sharedPreferences1.getString("redirect_uri", null));


        assertEquals(Config.getInstance().getPins(), Collections.singletonList("9hNxmEFgLKGJXqgp61hyb8yIyiT9u0vgDZh4y8TmY/M="));
        assertEquals(Config.getInstance().getBuildSteps(), Collections.emptyList());
        assertEquals(Config.getInstance().getAuthServiceName(), "Test");
        assertEquals(Config.getInstance().getRegistrationServiceName(), "Registration");

        assertEquals(Config.getInstance().getCookieName(), "iPlanetDirectoryPro");
        assertEquals(Config.getInstance().getRealm(), "root");
        assertEquals(Config.getInstance().getTimeout(), 30);
        assertEquals(Config.getInstance().getUrl(), "https://openam.example.com:8081/openam");

        assertEquals(Config.getInstance().getRedirectUri(), "https://www.example.com:8080/callback");
        assertEquals(Config.getInstance().getScope(), "openid email address");
        assertEquals(Config.getInstance().getClientId(), "andy_app");
        assertEquals(Config.getInstance().getOauthCacheMillis(), Long.valueOf(0));
        assertEquals(Config.getInstance().getOauthThreshold(), Long.valueOf(30));
        assertEquals(Config.getInstance().getCookieCacheMillis(), Long.valueOf(0));

    }
}
