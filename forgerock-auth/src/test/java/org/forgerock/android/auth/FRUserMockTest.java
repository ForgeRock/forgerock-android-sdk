/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.callback.StringAttributeInputCallback;
import org.forgerock.android.auth.callback.ValidatedCreatePasswordCallback;
import org.forgerock.android.auth.callback.ValidatedCreateUsernameCallback;
import org.forgerock.android.auth.exception.AlreadyAuthenticatedException;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.hamcrest.collection.IsIn;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class FRUserMockTest extends BaseTest {

    private static final String DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest";

    @Test
    public void frUserHappyPath() throws InterruptedException, ExecutionException, MalformedURLException, ParseException, JSONException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

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
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(ValidatedCreateUsernameCallback.class) != null) {
                    state.getCallback(ValidatedCreateUsernameCallback.class).setUsername("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(ValidatedCreatePasswordCallback.class) != null) {
                    state.getCallback(ValidatedCreatePasswordCallback.class).setPassword("password".toCharArray());
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
    public void testAccessTokenAsync() throws Exception {
        frUserHappyPath();
        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getAccessToken(future);
        assertNotNull(future.get());
    }

    @Test(expected = AuthenticationException.class)
    public void testUserInfoFailedDueToTokenExpired() throws Throwable {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_failed.json", HttpURLConnection.HTTP_UNAUTHORIZED);

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

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
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_failed.json", HttpURLConnection.HTTP_UNAUTHORIZED);

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

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
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_failed.json", HttpURLConnection.HTTP_UNAUTHORIZED);

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

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

        assertNotNull(nodeListenerFuture.get());

        //AppB

        setFinalStatic(FRUser.class.getDeclaredField("current"), null);
        Config.getInstance().getTokenManager().clear();
        assertNotNull(FRUser.getCurrentUser());
    }

    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        field.set(null, newValue);
    }

    @Test
    public void testLogout() throws InterruptedException, ExecutionException, MalformedURLException, JSONException, ParseException, AuthenticationRequiredException {
        frUserHappyPath();
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);

        RecordedRequest rr = server.takeRequest(); //Start the Auth Service POST /json/realms/root/authenticate?authIndexType=service&authIndexValue=UsernamePassword HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/json/realms/root/authenticate?authIndexType=service&authIndexValue=UsernamePassword");
        rr = server.takeRequest(); //Post Name Callback POST /json/realms/root/authenticate HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/json/realms/root/authenticate");
        rr = server.takeRequest(); //Post Password Callback POST /json/realms/root/authenticate HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/json/realms/root/authenticate");
        rr = server.takeRequest(); //Post to /authorize endpoint GET /oauth2/realms/root/authorize?iPlanetDirectoryPro=C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*&client_id=andy_app&scope=openid&response_type=code&redirect_uri=https%3A%2F%2Fwww.example.com%3A8080%2Fcallback&code_challenge=PnQUh9V3GPr5qamcKZ39fcv4o81KJbhYls89L5rkVs8&code_challenge_method=S256 HTTP/1.1
        Assertions.assertThat(rr.getPath()).startsWith("/oauth2/realms/root/authorize");
        rr = server.takeRequest(); //Post to /access-token endpoint POST /oauth2/realms/root/access_token HTTP/1.1
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
        List<String> result = new ArrayList<>();
        result.add("/json/realms/root/sessions?_action=logout");
        result.add("/oauth2/realms/root/token/revoke");
        assertThat(revoke1.getPath(), new IsIn(result));
        assertThat(revoke2.getPath(), new IsIn(result));

        RecordedRequest refreshTokenRevoke;
        RecordedRequest ssoTokenRevoke;
        if (revoke1.getPath().equals("/json/realms/root/sessions?_action=logout")) {
            ssoTokenRevoke = revoke1;
            refreshTokenRevoke = revoke2;
        } else {
            refreshTokenRevoke = revoke1;
            ssoTokenRevoke = revoke2;
        }
        assertNotNull(ssoTokenRevoke.getHeader(SSOToken.IPLANET_DIRECTORY_PRO));
        assertEquals(ServerConfig.API_VERSION_3_1, ssoTokenRevoke.getHeader(ServerConfig.ACCEPT_API_VERSION));

        String body = refreshTokenRevoke.getBody().readUtf8();
        assertTrue(body.contains(OAuth2.TOKEN));
        assertTrue(body.contains(OAuth2.CLIENT_ID));
        assertTrue(body.contains(accessToken.getRefreshToken()));

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

        RecordedRequest rr = server.takeRequest(); //Start the Auth Service
        rr = server.takeRequest(); //Post Name Callback
        rr = server.takeRequest(); //Post Password Callback
        rr = server.takeRequest(); //Post to /authorize endpoint
        rr = server.takeRequest(); //Post to /access-token endpoint
        rr = server.takeRequest(); //Post to /user-info endpoint

        assertNotNull(FRUser.getCurrentUser());
        FRUser.getCurrentUser().logout();
        assertNull(FRUser.getCurrentUser());

        assertFalse(Config.getInstance().getSessionManager().hasSession());
        rr = server.takeRequest(); //Post to oauth2/realms/root/token/revoke
        boolean verified = false;
        if (rr.getPath().equals("/json/realms/root/sessions?_action=logout")) {
            assertNotNull(rr.getHeader(SSOToken.IPLANET_DIRECTORY_PRO));
            assertEquals(ServerConfig.API_VERSION_3_1, rr.getHeader(ServerConfig.ACCEPT_API_VERSION));
            verified = true;
        }
        rr = server.takeRequest(); //Post to /sessions?_action=logout endpoint
        //assertEquals("/json/realms/root/sessions?_action=logout", rr.getPath());
        if (rr.getPath().equals("/json/realms/root/sessions?_action=logout")) {
            assertNotNull(rr.getHeader(SSOToken.IPLANET_DIRECTORY_PRO));
            assertEquals(ServerConfig.API_VERSION_3_1, rr.getHeader(ServerConfig.ACCEPT_API_VERSION));
            verified = true;
        }
        assertTrue(verified);
    }

    @Test
    public void testRevokeWithAccessToken() throws ExecutionException, InterruptedException, AuthenticationRequiredException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken_no_RefreshToken.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));


        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

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
        assertNotNull(nodeListenerFuture.get());

        AccessToken accessToken = FRUser.getCurrentUser().getAccessToken();

        //Mock the SSO Token is not stored
        Config.getInstance().getSingleSignOnManager().clear();

        FRUser.getCurrentUser().logout();
        RecordedRequest rr = server.takeRequest(); //Start the Auth Service
        rr = server.takeRequest(); //Post Name Callback
        rr = server.takeRequest(); //Post Password Callback
        rr = server.takeRequest(); //Post to /authorize endpoint
        rr = server.takeRequest(); //Post to /access-token endpoint
        rr = server.takeRequest(); //Post to /oauth2/realms/root/token/revoke

        String body = rr.getBody().readUtf8();
        assertTrue(body.contains(OAuth2.TOKEN));
        assertTrue(body.contains(OAuth2.CLIENT_ID));
        //Using the Access Token to revoke
        assertTrue(body.contains(accessToken.getValue()));


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

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());


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
                .context(context)
                .build();

        //Check AccessToken Storage
        AccessToken accessToken = FRUser.getCurrentUser().getAccessToken();
        assertNotNull(accessToken);
        assertNotNull(accessToken.getValue());

        //Check SSOToken Storage
        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .context(context)
                .build();
        Token token = singleSignOnManager.getToken();
        assertNotNull(token);
        assertNotNull(token.getValue());

        //Clear the Access Token
        tokenManager.clear();

        accessToken = FRUser.getCurrentUser().getAccessToken();
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

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

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

        Assert.assertTrue(nodeListenerFuture.get() instanceof FRUser);

        RecordedRequest rr = server.takeRequest(); //Start the Auth Service
        rr = server.takeRequest(); //Post Name Callback
        rr = server.takeRequest(); //Post Password Callback
        rr = server.takeRequest(); //Post to /authorize endpoint
        rr = server.takeRequest(); //Post to /access-token endpoint

        //Switch to another App with Access Token does not exists
        final TokenManager tokenManager = DefaultTokenManager.builder()
                .context(context)
                .build();
        tokenManager.clear();

        assertNotNull(FRUser.getCurrentUser().getAccessToken());
        rr = server.takeRequest(); //Post to /authorize endpoint
        rr = server.takeRequest(); //Post to /access-token endpoint

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
        assertTrue(Config.getInstance(context).getTokenManager().hasToken());


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
        assertFalse(Config.getInstance(context).getTokenManager().hasToken());

    }

    @Test
    public void testSessionTokenMatch() throws InterruptedException, ExecutionException, MalformedURLException, JSONException, ParseException {
        frUserHappyPath();

        //We have Access Token now.
        assertTrue(Config.getInstance(context).getTokenManager().hasToken());

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
        assertTrue(Config.getInstance(context).getTokenManager().hasToken());

    }

    @Test
    public void testSessionTokenUpdated() throws InterruptedException, ExecutionException, MalformedURLException, JSONException, ParseException, AuthenticationRequiredException {
        frUserHappyPath();
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));

        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);


        SingleSignOnManager singleSignOnManager = Config.getInstance(context).getSingleSignOnManager();
        TokenManager tokenManager = Config.getInstance(context).getTokenManager();
        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        tokenManager.getAccessToken(null, future);

        //Make sure the access token is bound to the Session Token
        assertEquals(singleSignOnManager.getToken(), future.get().getSessionToken());

        //Change the SSO Token, it can be done by SSO scenario.
        singleSignOnManager.persist(new SSOToken("New Dummy SSO Token"));

        FRUser.getCurrentUser().getAccessToken();

    }

    @Test
    public void testCustomEndpoint() throws InterruptedException, ExecutionException, MalformedURLException, JSONException, ParseException, AuthenticationRequiredException {
        Config.getInstance(context).setAuthenticateEndpoint("dummy/authenticate");
        Config.getInstance(context).setAuthorizeEndpoint("dummy/authorize");
        Config.getInstance(context).setTokenEndpoint("dummy/token");
        Config.getInstance(context).setUserinfoEndpoint("dummy/userinfo");
        Config.getInstance(context).setRevokeEndpoint("dummy/revoke");
        Config.getInstance(context).setLogoutEndpoint("dummy/logout");
        frUserHappyPath();
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);

        RecordedRequest rr = server.takeRequest(); //Start the Auth Service POST /json/realms/root/authenticate?authIndexType=service&authIndexValue=UsernamePassword HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/dummy/authenticate?authIndexType=service&authIndexValue=UsernamePassword");
        rr = server.takeRequest(); //Post Name Callback POST /json/realms/root/authenticate HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/dummy/authenticate");
        rr = server.takeRequest(); //Post Password Callback POST /json/realms/root/authenticate HTTP/1.1
        Assertions.assertThat(rr.getPath()).isEqualTo("/dummy/authenticate");
        rr = server.takeRequest(); //Post to /authorize endpoint GET /oauth2/realms/root/authorize?iPlanetDirectoryPro=C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*&client_id=andy_app&scope=openid&response_type=code&redirect_uri=https%3A%2F%2Fwww.example.com%3A8080%2Fcallback&code_challenge=PnQUh9V3GPr5qamcKZ39fcv4o81KJbhYls89L5rkVs8&code_challenge_method=S256 HTTP/1.1
        Assertions.assertThat(rr.getPath()).startsWith("/dummy/authorize");
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

        //revoke Refresh Token and SSO Token are performed async
        List<String> result = new ArrayList<>();
        result.add("/dummy/logout?_action=logout");
        result.add("/dummy/revoke");
        assertThat(revoke1.getPath(), new IsIn(result));
        assertThat(revoke2.getPath(), new IsIn(result));

        RecordedRequest refreshTokenRevoke;
        RecordedRequest ssoTokenRevoke;
        if (revoke1.getPath().equals("/dummy/logout?_action=logout")) {
            ssoTokenRevoke = revoke1;
            refreshTokenRevoke = revoke2;
        } else {
            refreshTokenRevoke = revoke1;
            ssoTokenRevoke = revoke2;
        }
        assertNotNull(ssoTokenRevoke.getHeader(SSOToken.IPLANET_DIRECTORY_PRO));
        assertEquals(ServerConfig.API_VERSION_3_1, ssoTokenRevoke.getHeader(ServerConfig.ACCEPT_API_VERSION));

        String body = refreshTokenRevoke.getBody().readUtf8();
        assertTrue(body.contains(OAuth2.TOKEN));
        assertTrue(body.contains(OAuth2.CLIENT_ID));
        assertTrue(body.contains(accessToken.getRefreshToken()));
    }
}
