/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.callback.StringAttributeInputCallback;
import org.forgerock.android.auth.callback.ValidatedPasswordCallback;
import org.forgerock.android.auth.callback.ValidatedUsernameCallback;
import org.forgerock.android.auth.exception.AlreadyAuthenticatedException;
import org.forgerock.android.auth.exception.ApiException;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import okhttp3.Cookie;

@RunWith(RobolectricTestRunner.class)
public class FRUserMockTest extends BaseTest {

    private static final String DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest";
    private static final String DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest";

    @Mock
    SSOBroadcastModel mockBroadcastModel;

    @Before
    public void setUp() throws Exception {
        Config.getInstance().setSSOBroadcastModel(mockBroadcastModel);
        when(mockBroadcastModel.isBroadcastEnabled()).thenReturn(true);
    }

    @Test
    public void frUserHappyPath() throws InterruptedException, ExecutionException, MalformedURLException, ParseException, JSONException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);

        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest recordedRequest = server.takeRequest();
        String state = Uri.parse(recordedRequest.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK);

        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        FRListenerFuture<UserInfo> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getUserInfo(future);
        UserInfo userinfo = future.get();

        assertEquals("sub", userinfo.getSub());
        assertEquals("name", userinfo.getName());
        assertEquals("given name", userinfo.getGivenName());
        assertEquals("family name", userinfo.getFamilyName());
        assertEquals("middle name", userinfo.getMiddleName());
        assertEquals("nick name", userinfo.getNickName());
        assertEquals("preferred username", userinfo.getPreferredUsername());
        assertEquals(new URL("http://profile"), userinfo.getProfile());
        assertEquals(new URL("http://picture"), userinfo.getPicture());
        assertEquals(new URL("http://website"), userinfo.getWebsite());
        assertEquals("test@email.com", userinfo.getEmail());
        assertEquals(true, userinfo.getEmailVerified());
        assertEquals("male", userinfo.getGender());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        assertEquals(simpleDateFormat.parse("2008-01-30"), userinfo.getBirthDate());
        assertEquals("zoneinfo", userinfo.getZoneInfo());
        assertEquals("locale", userinfo.getLocale());
        assertEquals("phone number", userinfo.getPhoneNumber());
        assertEquals(true, userinfo.getPhoneNumberVerified());
        assertEquals("800000", userinfo.getUpdateAt().toString());
        assertEquals("formatted", userinfo.getAddress().getFormatted());
        assertEquals("street address", userinfo.getAddress().getStreetAddress());
        assertEquals("locality", userinfo.getAddress().getLocality());
        assertEquals("region", userinfo.getAddress().getRegion());
        assertEquals("90210", userinfo.getAddress().getPostalCode());
        assertEquals("US", userinfo.getAddress().getCountry());
        assertEquals(getJson("/userinfo_success.json"), userinfo.getRaw().toString(2));

    }

    @Ignore("For now not to cache userinfo in memory")
    @Test
    public void userInfoIsCached() throws InterruptedException, ExecutionException, MalformedURLException, ParseException, JSONException {
        frUserHappyPath();
        //No userinfo enqueued
        FRListenerFuture<UserInfo> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getUserInfo(future);
        future.get();

    }

    /**
     * Start -> Platform Username -> Platform Password -> Attribute Collector -> Create Object
     */
    @Test
    public void frAuthRegistrationHappyPath() throws InterruptedException, ExecutionException, JSONException, ParseException, MalformedURLException {

        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));

        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(ValidatedUsernameCallback.class) != null) {
                    state.getCallback(ValidatedUsernameCallback.class).setUsername("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(ValidatedPasswordCallback.class) != null) {
                    state.getCallback(ValidatedPasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                    return;
                }

                List<Callback> callbacks = state.getCallbacks();
                StringAttributeInputCallback email = ((StringAttributeInputCallback) callbacks.get(0));
                StringAttributeInputCallback firstName = ((StringAttributeInputCallback) callbacks.get(1));
                StringAttributeInputCallback lastName = ((StringAttributeInputCallback) callbacks.get(2));
                email.setValue("test@test.com");
                firstName.setValue("My First Name");
                lastName.setValue("My Last Name");
                state.next(context, this);

            }
        };

        FRUser.register(context, nodeListenerFuture);
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest recordedRequest = server.takeRequest();
        String state = Uri.parse(recordedRequest.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK);

        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

    }

    @Test
    public void testAccessToken() throws Exception {
        frUserHappyPath();
        AccessToken accessToken = FRUser.getCurrentUser().getAccessToken();
        assertNotNull(accessToken.getValue());
    }

    @Test
    public void testRevokeAccessToken() throws Exception {
        frUserHappyPath();
        //revoke Access Token
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));

        FRListenerFuture<Void> future = new FRListenerFuture<>();
        assertNotNull(FRUser.getCurrentUser());
        //Check if the token exists
        assertTrue(Config.getInstance().getTokenManager().hasToken());
        //Revoke the token
        FRUser.getCurrentUser().revokeAccessToken(future);
        try {
            future.get();
        } catch (ExecutionException e) {
            //Timeout exception expected
            assertEquals("java.net.SocketTimeoutException: timeout", e.getMessage());
        }
        //Check that the token has been cleared
        assertFalse(Config.getInstance().getTokenManager().hasToken());
    }

    @Test
    public void testAccessTokenAsync() throws Exception {
        frUserHappyPath();
        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getAccessToken(future);
        assertNotNull(future.get());
    }

    @Test
    public void testRefreshTokenAsync() throws Exception {
        frUserHappyPath();
        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getAccessToken(future);
        assertNotNull(future.get());
        //server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK));
        //For Asyn revoke
        enqueue("/authTreeMockTest_Authenticate_accessToken_no_RefreshToken_shortlife.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_accessToken_no_RefreshToken_shortlife.json", HttpURLConnection.HTTP_OK);
        //For revoke existing Access Token
        FRListenerFuture<AccessToken> refreshTokenFuture = new FRListenerFuture<>();
        FRUser.getCurrentUser().refresh(refreshTokenFuture);
        server.takeRequest();
        assertNotEquals(future.get().getExpiresIn(), refreshTokenFuture.get().getExpiresIn());
        assertNotEquals(future.get().getValue(), refreshTokenFuture.get().getValue());
        assertNotEquals(future.get().getIdToken(), refreshTokenFuture.get().getIdToken());
    }

    @Test(expected = AuthenticationException.class)
    public void testUserInfoFailedDueToTokenExpired() throws Throwable {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));

        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest recordedRequest = server.takeRequest();
        String state = Uri.parse(recordedRequest.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_failed.json", HttpURLConnection.HTTP_UNAUTHORIZED);


        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        FRListenerFuture<UserInfo> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getUserInfo(future);
        try {
            future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }


    }

    @Test(expected = AuthenticationException.class)
    public void testUserInfoFailedDueToTokenRemoved() throws Throwable {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));

        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);

        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest recordedRequest = server.takeRequest();
        String state = Uri.parse(recordedRequest.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_failed.json", HttpURLConnection.HTTP_UNAUTHORIZED);


        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        FRUser temp = FRUser.getCurrentUser();
        temp.logout();
        Thread.sleep(10); //Make sure trigger the logout first to dequeue the correct message from server
        FRListenerFuture<UserInfo> future = new FRListenerFuture<>();
        temp.getUserInfo(future);
        try {
            future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testWithSSO() throws Throwable {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));

        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest recordedRequest = server.takeRequest();
        String state = Uri.parse(recordedRequest.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_failed.json", HttpURLConnection.HTTP_UNAUTHORIZED);

        assertNotNull(nodeListenerFuture.get());

        //AppB

        //setFinalStatic(FRUser.class.getDeclaredField("current"), null);
        EventDispatcher.TOKEN_REMOVED.notifyObservers();
        Config.getInstance().getTokenManager().clear();
        assertNotNull(FRUser.getCurrentUser());
    }

    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        field.set(null, newValue);
    }

    @Test
    public void testLogout() throws InterruptedException, ExecutionException, IOException, JSONException, ParseException, AuthenticationRequiredException, ApiException {
        frUserHappyPath();
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);

        RecordedRequest rr = server.takeRequest(); //Start the Auth Service POST /json/realms/root/authenticate?authIndexType=service&authIndexValue=Test HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/oauth2/realms/root/access_token");
        rr = server.takeRequest(); //Post to /user-info endpoint GET /oauth2/realms/root/userinfo HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/oauth2/realms/root/userinfo");

        AccessToken accessToken = FRUser.getCurrentUser().getAccessToken();
        assertNotNull(FRUser.getCurrentUser());
        FRUser.getCurrentUser().logout();
        assertNull(FRUser.getCurrentUser());

        assertFalse(Config.getInstance().getSessionManager().hasSession());

        RecordedRequest revoke1 = server.takeRequest(); //Post to oauth2/realms/root/token/revoke
        RecordedRequest revoke2 = server.takeRequest(); //Post to /sessions?_action=logout endpoint

        //revoke Refresh Token and SSO Token are performed async
        RecordedRequest ssoTokenRevoke = findRequest("/json/realms/root/sessions?_action=logout", revoke1, revoke2);
        RecordedRequest refreshTokenRevoke = findRequest("/oauth2/realms/root/token/revoke", revoke1, revoke2);

        assertNotNull(ssoTokenRevoke.getHeader(serverConfig.getCookieName()));
        assertEquals(ServerConfig.API_VERSION_3_1, ssoTokenRevoke.getHeader(ServerConfig.ACCEPT_API_VERSION));

        String body = refreshTokenRevoke.getBody().readUtf8();
        assertTrue(body.contains(OAuth2.TOKEN));
        assertTrue(body.contains(OAuth2.CLIENT_ID));
        assertTrue(body.contains(accessToken.getRefreshToken()));
        verify(mockBroadcastModel).sendLogoutBroadcast();
    }

    private RecordedRequest findRequest(String path, RecordedRequest... recordedRequests) {
        for (RecordedRequest r : recordedRequests) {
            if (r.getPath().startsWith(path)) {
                return r;
            }
        }
        throw new IllegalArgumentException();
    }

    @Test
    public void testLogoutFailed() throws InterruptedException, ExecutionException, MalformedURLException, JSONException, ParseException {
        frUserHappyPath();
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .addHeader("Content-Type", "application/json")
                .setBody("{\n" +
                        "    \"error_description\": \"Client authentication failed\",\n" +
                        "    \"error\": \"invalid_client\"\n" +
                        "}"));
        enqueue("/sessions_logout_failed.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_NO_CONTENT));

        RecordedRequest rr = server.takeRequest(); //Post to /access-token endpoint
        rr = server.takeRequest();//Post to /user-info endpoint

        assertNotNull(FRUser.getCurrentUser());
        FRUser.getCurrentUser().logout();
        assertNull(FRUser.getCurrentUser());

        assertFalse(Config.getInstance().getSessionManager().hasSession());
        RecordedRequest revoke1 = server.takeRequest(); //Post to oauth2/realms/root/token/revoke
        RecordedRequest revoke2 = server.takeRequest(); //Post to /sessions?_action=logout endpoint

        rr = findRequest("/json/realms/root/sessions?_action=logout", revoke1, revoke2);
        assertNotNull(rr.getHeader(serverConfig.getCookieName()));
        assertEquals(ServerConfig.API_VERSION_3_1, rr.getHeader(ServerConfig.ACCEPT_API_VERSION));
    }

    @Test
    public void testRevokeWithAccessToken() throws ExecutionException, InterruptedException, AuthenticationRequiredException, IOException, ApiException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);


        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest recordedRequest = server.takeRequest();
        String state = Uri.parse(recordedRequest.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));

        enqueue("/authTreeMockTest_Authenticate_accessToken_no_RefreshToken.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));


        assertNotNull(nodeListenerFuture.get());

        AccessToken accessToken = FRUser.getCurrentUser().getAccessToken();

        //Mock the SSO Token is not stored
        Config.getInstance().getSingleSignOnManager().clear();

        FRUser.getCurrentUser().logout();
        RecordedRequest rr = server.takeRequest(); ///Post to /access-token endpoint
        rr = server.takeRequest();//Post to /oauth2/realms/root/token/revoke

        String body = rr.getBody().readUtf8();
        assertTrue(body.contains(OAuth2.TOKEN));
        assertTrue(body.contains(OAuth2.CLIENT_ID));
        //Using the Access Token to revoke
        assertTrue(body.contains(accessToken.getValue()));


    }

    @Test
    public void testAccessTokenAndSSOTokenRefreshWithSSOToken() throws ExecutionException, InterruptedException, AuthenticationRequiredException, IOException, ApiException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        SharedPreferences tokenManagerSharedPreferences = context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE);
        SharedPreferences ssoManagerSharedPreferences = context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE);
        Config.getInstance().setSharedPreferences(tokenManagerSharedPreferences);
        Config.getInstance().setSsoSharedPreferences(ssoManagerSharedPreferences);

        Config.getInstance().setUrl(getUrl());


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
        FRSession.authenticate(context, "Example", nodeListenerFuture);

        Assert.assertTrue(nodeListenerFuture.get() instanceof FRSession);

        final TokenManager tokenManager = DefaultTokenManager.builder()
                .sharedPreferences(tokenManagerSharedPreferences)
                .context(context)
                .build();

        //Check AccessToken Storage
        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getAccessToken(future);
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();

        RecordedRequest recordedRequest = server.takeRequest();
        String state = Uri.parse(recordedRequest.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        AccessToken accessToken = future.get();
        assertNotNull(accessToken);
        assertNotNull(accessToken.getValue());

        //Check SSOToken Storage
        final SingleSignOnManager singleSignOnManager = Config.getInstance().getSingleSignOnManager();
        Token token = singleSignOnManager.getToken();
        assertNotNull(token);
        assertNotNull(token.getValue());

        //Clear the Access Token
        tokenManager.clear();


        future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getAccessToken(future);
        server.takeRequest(); // /token endpoint
        recordedRequest = server.takeRequest();
        state = Uri.parse(recordedRequest.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        assertNotNull(future.get().getValue());

    }

    @Test
    public void testSSOEnabled() throws ExecutionException, InterruptedException, AuthenticationRequiredException, IOException, ApiException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));

        Config.getInstance().setUrl(getUrl());

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

        FRUser.login(context, nodeListenerFuture);

        RecordedRequest rr = server.takeRequest(); //Start the Auth Service
        rr = server.takeRequest(); //Post Name Callback
        rr = server.takeRequest(); //Post Password Callback
        rr = server.takeRequest(); //Post to /authorize endpoint
        String state = Uri.parse(rr.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        rr = server.takeRequest(); //Post to /access-token endpoint

        Assert.assertTrue(nodeListenerFuture.get() instanceof FRUser);

        //Switch to another App with Access Token does not exists
        final TokenManager tokenManager = DefaultTokenManager.builder()
                .sharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE))
                .context(context)
                .build();
        tokenManager.clear();

        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getAccessToken(future);
        rr = server.takeRequest(); //Post to /access-token endpoint
        state = Uri.parse(rr.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        assertNotNull(future.get());

    }

    @Test(expected = AlreadyAuthenticatedException.class)
    public void testRelogin() throws Throwable {
        frUserHappyPath();
        NodeListenerFuture<FRUser> listener = new NodeListenerFuture<FRUser>() {
            @Override
            public void onCallbackReceived(Node node) {

            }
        };
        FRUser.login(context, listener);
        try {
            listener.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test(expected = AlreadyAuthenticatedException.class)
    public void testReregister() throws Throwable {
        frUserHappyPath();

        NodeListenerFuture<FRUser> listener = new NodeListenerFuture<FRUser>() {
            @Override
            public void onCallbackReceived(Node node) {

            }
        };
        FRUser.register(context, listener);
        try {
            listener.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testSessionTokenMismatch() throws InterruptedException, ExecutionException, MalformedURLException, JSONException, ParseException {
        frUserHappyPath();

        //We have Access Token now.
        assertTrue(Config.getInstance().getTokenManager().hasToken());


        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success2.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

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

        FRSession.authenticate(context, "any", nodeListenerFuture);
        FRSession session = nodeListenerFuture.get();
        assertEquals("dummy sso token", session.getSessionToken().getValue());

        //Access Token should be removed
        assertFalse(Config.getInstance().getTokenManager().hasToken());

    }

    @Test
    public void testSessionTokenMatch() throws InterruptedException, ExecutionException, MalformedURLException, JSONException, ParseException {
        frUserHappyPath();

        //We have Access Token now.
        assertTrue(Config.getInstance().getTokenManager().hasToken());

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        //Return Same SSO Token
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

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

        FRSession.authenticate(context, "any", nodeListenerFuture);
        nodeListenerFuture.get();

        //Access Token should not be removed
        assertTrue(Config.getInstance().getTokenManager().hasToken());

    }

    @Test
    public void testSessionTokenUpdated() throws InterruptedException, ExecutionException, IOException, JSONException, ParseException, AuthenticationRequiredException, ApiException {
        frUserHappyPath();

        SingleSignOnManager singleSignOnManager = Config.getInstance().getSingleSignOnManager();
        TokenManager tokenManager = Config.getInstance().getTokenManager();
        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        tokenManager.getAccessToken(null, future);


        //Make sure the access token is bounded to the Session Token
        assertEquals(singleSignOnManager.getToken(), future.get().getSessionToken());

        //Change the SSO Token, it can be done by SSO scenario.
        singleSignOnManager.persist(new SSOToken("New Dummy SSO Token"));

        future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getAccessToken(future);

        //Since the sso token changed, revoke Refresh Token
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));


        //Using new Session Token to get AccessToken
        server.takeRequest(); //token
        server.takeRequest(); //userinfo
        server.takeRequest(); //revoke
        RecordedRequest rr = server.takeRequest(); //Post to /authorize endpoint
        String state = Uri.parse(rr.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        assertEquals(singleSignOnManager.getToken(), future.get().getSessionToken());

    }

    @Test
    public void testCustomEndpointAndCookieName() throws InterruptedException, ExecutionException, AuthenticationRequiredException {

        when(mockContext.getString(R.string.forgerock_oauth_client_id)).thenReturn(context.getString(R.string.forgerock_oauth_client_id));
        when(mockContext.getString(R.string.forgerock_oauth_redirect_uri)).thenReturn(context.getString(R.string.forgerock_oauth_redirect_uri));
        when(mockContext.getString(R.string.forgerock_oauth_scope)).thenReturn(context.getString(R.string.forgerock_oauth_scope));
        when(mockContext.getString(R.string.forgerock_oauth_url)).thenReturn(context.getString(R.string.forgerock_oauth_url));
        when(mockContext.getString(R.string.forgerock_realm)).thenReturn(context.getString(R.string.forgerock_realm));
        Resources resources = Mockito.mock(Resources.class);
        when(mockContext.getResources()).thenReturn(resources);
        when(mockContext.getString(R.string.forgerock_url)).thenReturn("https://dummy.com");
        when(mockContext.getString(R.string.forgerock_realm)).thenReturn("root");
        when(mockContext.getString(R.string.forgerock_registration_service)).thenReturn("registration");
        when(mockContext.getApplicationContext()).thenReturn(context);
        when(resources.getInteger(R.integer.forgerock_timeout)).thenReturn(30);
        when(resources.getStringArray(R.array.forgerock_ssl_pinning_public_key_hashes)).thenReturn(context.getResources().getStringArray(R.array.forgerock_ssl_pinning_public_key_hashes));
        when(mockContext.getString(R.string.forgerock_authenticate_endpoint)).thenReturn("dummy/authenticate");
        when(mockContext.getString(R.string.forgerock_authorize_endpoint)).thenReturn("dummy/authorize");
        when(mockContext.getString(R.string.forgerock_token_endpoint)).thenReturn("dummy/token");
        when(mockContext.getString(R.string.forgerock_userinfo_endpoint)).thenReturn("dummy/userinfo");
        when(mockContext.getString(R.string.forgerock_revoke_endpoint)).thenReturn("dummy/revoke");
        when(mockContext.getString(R.string.forgerock_session_endpoint)).thenReturn("dummy/logout");
        when(mockContext.getString(R.string.forgerock_endsession_endpoint)).thenReturn("dummy/endSession");
        when(mockContext.getString(R.string.forgerock_cookie_name)).thenReturn("testCookieName");
        when(mockContext.getString(R.string.forgerock_auth_service)).thenReturn("UsernamePassword");

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.reset();
        Config.getInstance().init(mockContext, null);
        serverConfig = Config.getInstance().getServerConfig();

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(mockContext, nodeListenerFuture);

        RecordedRequest rr = server.takeRequest(); //Start the Auth Service POST /json/realms/root/authenticate?authIndexType=service&authIndexValue=UsernamePassword HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/dummy/authenticate?authIndexType=service&authIndexValue=UsernamePassword");
        rr = server.takeRequest(); //Post Name Callback POST /json/realms/root/authenticate HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/dummy/authenticate");
        rr = server.takeRequest(); //Post Password Callback POST /json/realms/root/authenticate HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/dummy/authenticate");
        rr = server.takeRequest(); //Post to /authorize endpoint GET /oauth2/realms/root/authorize?iPlanetDirectoryPro=C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*&client_id=andy_app&scope=openid&response_type=code&redirect_uri=https%3A%2F%2Fwww.example.com%3A8080%2Fcallback&code_challenge=PnQUh9V3GPr5qamcKZ39fcv4o81KJbhYls89L5rkVs8&code_challenge_method=S256 HTTP/1.1
        Assertions.assertThat(rr.getPath()).startsWith("/dummy/authorize");

        String state = Uri.parse(rr.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK);

        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        FRListenerFuture<UserInfo> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getUserInfo(future);
        future.get();

        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_NO_CONTENT));

        rr = server.takeRequest(); //Post to /access-token endpoint POST /oauth2/realms/root/access_token HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/dummy/token");
        rr = server.takeRequest(); //Post to /user-info endpoint GET /oauth2/realms/root/userinfo HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/dummy/userinfo");

        AccessToken accessToken = FRUser.getCurrentUser().getAccessToken();
        assertNotNull(FRUser.getCurrentUser());

        FRUser.getCurrentUser().logout();
        assertNull(FRUser.getCurrentUser());

        assertFalse(Config.getInstance().getSessionManager().hasSession());

        RecordedRequest revoke1 = server.takeRequest(); //Post to oauth2/realms/root/token/revoke
        RecordedRequest revoke2 = server.takeRequest(); //Post to /sessions?_action=logout endpoint

        RecordedRequest ssoTokenRevoke = findRequest("/dummy/logout?_action=logout", revoke1, revoke2);
        RecordedRequest refreshTokenRevoke = findRequest("/dummy/revoke", revoke1, revoke2);

        Assertions.assertThat(ssoTokenRevoke.getHeader("testCookieName")).isNotNull();
        assertEquals(ServerConfig.API_VERSION_3_1, ssoTokenRevoke.getHeader(ServerConfig.ACCEPT_API_VERSION));

        String body = refreshTokenRevoke.getBody().readUtf8();
        assertTrue(body.contains(OAuth2.TOKEN));
        assertTrue(body.contains(OAuth2.CLIENT_ID));
        assertTrue(body.contains(accessToken.getRefreshToken()));

    }

    @Test
    public void testRequestInterceptor() throws InterruptedException, ExecutionException, MalformedURLException, JSONException, ParseException {

        //Total e request will be intercepted
        CountDownLatch countDownLatch = new CountDownLatch(8);
        final HashMap<String, Pair<Action, Integer>> result = new HashMap<>();
        RequestInterceptorRegistry.getInstance().register(request -> {
            countDownLatch.countDown();
            String action = ((Action) request.tag()).getType();
            Pair<Action, Integer> pair = result.get(action);
            if (pair == null) {
                result.put(action, new Pair<>((Action) request.tag(), 1));
            } else {
                result.put(action, new Pair<>((Action) request.tag(), pair.second + 1));
            }
            return request;
        });

        frUserHappyPath();

        //Logout
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);


        FRUser.getCurrentUser().logout();
        countDownLatch.await();

        Assertions.assertThat(result.get("START_AUTHENTICATE").first.getPayload().getString("tree")).isEqualTo("Test");
        Assertions.assertThat(result.get("START_AUTHENTICATE").first.getPayload().getString("type")).isEqualTo("service");
        Assertions.assertThat(result.get("START_AUTHENTICATE").second).isEqualTo(1);
        Assertions.assertThat(result.get("AUTHENTICATE").first.getPayload().getString("tree")).isEqualTo("Test");
        Assertions.assertThat(result.get("AUTHENTICATE").first.getPayload().getString("type")).isEqualTo("service");
        Assertions.assertThat(result.get("AUTHENTICATE").second).isEqualTo(2);
        Assertions.assertThat(result.get("AUTHORIZE").second).isEqualTo(1);
        Assertions.assertThat(result.get("EXCHANGE_TOKEN").second).isEqualTo(1);
        Assertions.assertThat(result.get("REVOKE_TOKEN").second).isEqualTo(1);
        Assertions.assertThat(result.get("LOGOUT").second).isEqualTo(1);
        //Assertions.assertThat(result.get("END_SESSION").second).isEqualTo(1);
        Assertions.assertThat(result.get("USER_INFO").second).isEqualTo(1);
    }

    @Test
    public void testCookieInterceptor() throws InterruptedException, ExecutionException, MalformedURLException, JSONException, ParseException {

        //Total e request will be intercepted
        CountDownLatch countDownLatch = new CountDownLatch(8);
        RequestInterceptorRegistry.getInstance().register((CustomCookieInterceptor) cookies -> {
            countDownLatch.countDown();
            List<Cookie> customizedCookies = new ArrayList<>();
            customizedCookies.add(new Cookie.Builder().domain("localhost").name("test").value("testValue").build());
            customizedCookies.addAll(cookies);
            return customizedCookies;
        });

        frUserHappyPath();

        //Logout
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);


        FRUser.getCurrentUser().logout();
        countDownLatch.await();

        //Take few requests and make sure it contains the custom header.
        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getHeader("Cookie")).isEqualTo("test=testValue");
        recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getHeader("Cookie")).isEqualTo("test=testValue");
        recordedRequest = server.takeRequest();


    }


    @Test
    public void testAccessTokenWithExpiredRefreshToken() throws ExecutionException, InterruptedException, AuthenticationRequiredException, ApiException, IOException {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);

        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest rr = server.takeRequest();
        String state = Uri.parse(rr.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken_shortlife.json", HttpURLConnection.HTTP_OK);

        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        //Refresh failed due to invalid grant (Assumption that invalid grant = refresh token expired)
        server.enqueue(new MockResponse()
                .setBody("{\n" +
                        "    \"error_description\": \"grant is invalid\",\n" +
                        "    \"error\": \"invalid_grant\"\n" +
                        "}")
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST));
        //Let the token expire, so that we use the refresh token flow
        Thread.sleep(1000);

        server.takeRequest(); //access_token

        //Use the session token to retrieve the Access token
        //Assert that we retrieve the new token
        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getAccessToken(future);

        server.takeRequest(); //revoke
        rr = server.takeRequest();
        state = Uri.parse(rr.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        Assertions.assertThat(future.get().getExpiresIn()).isEqualTo(3599);
        Assertions.assertThat(FRSession.getCurrentSession()).isNotNull();

    }

    @Test
    public void testAccessTokenWithExpiredRefreshTokenFailedToRefreshWithSSOToken() throws ExecutionException, InterruptedException, ApiException, IOException {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);

        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest rr = server.takeRequest();
        String state = Uri.parse(rr.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken_shortlife.json", HttpURLConnection.HTTP_OK);

        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        //Refresh failed due to invalid grant (Assumption that invalid grant = refresh token expired)
        server.enqueue(new MockResponse()
                .setBody("{\n" +
                        "    \"error_description\": \"grant is invalid\",\n" +
                        "    \"error\": \"invalid_grant\"\n" +
                        "}")
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST));
        //Let the token expire, so that we use the refresh token flow
        Thread.sleep(1000);

        //Use the session token to retrieve the Access token
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST));

        try {
            FRUser.getCurrentUser().getAccessToken();
            fail();
        } catch (AuthenticationRequiredException e) {
        }

        Assertions.assertThat(FRUser.getCurrentUser()).isNull();
        Assertions.assertThat(FRSession.getCurrentSession()).isNull();

    }

    @Ignore("For 3.0")
    @Test
    public void testAccessTokenWithInvalidClientDuringRefresh() throws ExecutionException, InterruptedException, ApiException, IOException, AuthenticationRequiredException {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest rr = server.takeRequest();
        String state = Uri.parse(rr.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken_shortlife.json", HttpURLConnection.HTTP_OK);

        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        String error = "{\n" +
                "    \"error_description\": \"client is invalid\",\n" +
                "    \"error\": \"invalid_client\"\n" +
                "}";
        //Refresh failed due to invalid client
        server.enqueue(new MockResponse()
                .setBody(error)
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST));
        //Let the token expire, so that we use the refresh token flow
        Thread.sleep(1000);

        try {
            FRUser.getCurrentUser().getAccessToken();
            fail();
        } catch (Exception e) {
            ApiException exception = (ApiException) e;
            Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpURLConnection.HTTP_BAD_REQUEST);
            Assertions.assertThat(exception.getMessage()).isEqualTo(error);
        }

        Assertions.assertThat(FRUser.getCurrentUser()).isNotNull();
        Assertions.assertThat(FRSession.getCurrentSession()).isNotNull();
    }

    @Test
    public void testAccessTokenWithoutRefresh() throws ExecutionException, InterruptedException, ApiException, IOException, AuthenticationRequiredException {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest rr = server.takeRequest();
        String state = Uri.parse(rr.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken_no_RefreshToken_shortlife.json", HttpURLConnection.HTTP_OK);

        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        //Let the token expire, so that we use the refresh token flow
        Thread.sleep(1000);

        //Use the session token to retrieve the Access token
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST));

        try {
            FRUser.getCurrentUser().getAccessToken();
            fail();
        } catch (AuthenticationRequiredException e) {
        }

        Assertions.assertThat(FRUser.getCurrentUser()).isNull();
        Assertions.assertThat(FRSession.getCurrentSession()).isNull();

    }

    @Test
    public void testAccessTokenWithUnmatchSSOToken() throws ExecutionException, InterruptedException {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest rr = server.takeRequest();
        String state = Uri.parse(rr.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));

        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        //revoke Access Token
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));

        //endsession
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);


        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());
        //SSO Token has been updated, user may sing in with another user with a different SSOToken
        Config.getInstance().getSingleSignOnManager().persist(new SSOToken("UpdatedSSOToken"));

        try {
            FRUser.getCurrentUser().getAccessToken();
            fail();
        } catch (AuthenticationRequiredException e) {
            //expect exception
        }

        Assertions.assertThat(FRUser.getCurrentUser()).isNull();
        Assertions.assertThat(FRSession.getCurrentSession()).isNull();

    }

    @Test
    public void testAccessTokenRestoreSSOToken() throws ExecutionException, InterruptedException, AuthenticationRequiredException {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);

        server.takeRequest();
        server.takeRequest();
        server.takeRequest();

        RecordedRequest rr = server.takeRequest();
        String state = Uri.parse(rr.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);


        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());
        //Remove the SSOToken
        Config.getInstance().getSingleSignOnManager().revoke(null);

        Assertions.assertThat(FRUser.getCurrentUser().getAccessToken()).isNotNull();
        Assertions.assertThat(Config.getInstance().getSingleSignOnManager().getToken()).isNotNull();

    }

    private interface CustomCookieInterceptor extends FRRequestInterceptor<Action>, CookieInterceptor {
        @NonNull
        @Override
        default Request intercept(@NonNull Request request, Action tag) {
            return request;
        }
    }
}
