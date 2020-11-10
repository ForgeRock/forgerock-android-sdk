/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.exception.AlreadyAuthenticatedException;
import org.forgerock.android.auth.exception.ApiException;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.forgerock.android.auth.exception.BrowserAuthenticationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class BrowserLoginTest extends BaseTest {

    private static final String DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest";
    private static final String DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest";
    public static final String INVALID_SCOPE = "Invalid Scope";

    private AppAuthFragment getAppAuthFragment(FragmentActivity activity) {
        Fragment fragment = activity.getSupportFragmentManager()
                .findFragmentByTag(AppAuthFragment.TAG);
        if (fragment == null) {
            return null;
        }
        return (AppAuthFragment) fragment;
    }

    @Test
    public void testHappyPath() throws InterruptedException, ExecutionException, AuthenticationRequiredException, IOException, ApiException {
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());
        Config.getInstance().setEncryptor(new MockEncryptor());

        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        FRListenerFuture<FRUser> future = new FRListenerFuture<>();
        FRUser.browser().
                failedOnNoBrowserFound(false).
                login(fragmentActivity, future);
        AppAuthFragment appAuthFragment = getAppAuthFragment(fragmentActivity);
        Intent intent = new Intent();
        intent.putExtra(AuthorizationResponse.EXTRA_RESPONSE,
                "{\"request\":{\"configuration\":{\"authorizationEndpoint\":\"http:\\/\\/openam.example.com:8081\\/openam\\/oauth2\\/realms\\/root\\/authorize\",\"tokenEndpoint\":\"http:\\/\\/openam.example.com:8081\\/openam\\/oauth2\\/realms\\/root\\/access_token\"},\"clientId\":\"AndroidTest\",\"responseType\":\"code\",\"redirectUri\":\"net.openid.appauthdemo2:\\/oauth2redirect\",\"login_hint\":\"login\",\"scope\":\"openid profile email address phone\",\"state\":\"2v0SIhB7UAmsqvnvwR-IKQ\",\"codeVerifier\":\"qvCFoo3tqB-89lYOFjX2ZAMalkKgW_KIcc1tN3hmx3ygOHyYbWT9hKU7rhky6ivB-33exlhyyHHeSJtuVfOobg\",\"codeVerifierChallenge\":\"i-UW4h0UlD_pt1WCYGeP6prmtOkXhyQB_s1itrkV68k\",\"codeVerifierChallengeMethod\":\"S256\",\"additionalParameters\":{}},\"state\":\"2v0SIhB7UAmsqvnvwR-IKQ\",\"code\":\"roxwkG0TtooR2vzA6z0MT9xyJSQ\",\"additional_parameters\":{\"iss\":\"http:\\/\\/openam.example.com:8081\\/openam\\/oauth2\",\"client_id\":\"andy_app\"}}");
        appAuthFragment.onActivityResult(AppAuthFragment.AUTH_REQUEST_CODE, Activity.RESULT_OK, intent);
        FRUser frUser = future.get();
        assertThat(frUser.getAccessToken()).isNotNull();

        RecordedRequest rr = server.takeRequest(); //Start the Auth Service POST /json/realms/root/authenticate?authIndexType=service&authIndexValue=Test HTTP/1.1
        assertThat(rr.getPath()).isEqualTo("/oauth2/realms/root/access_token");
        assertThat(rr.getMethod()).isEqualTo("POST");
        Map<String, String> body = parse(rr.getBody().readUtf8());
        assertThat(body.get("client_id")).isEqualTo("andy_app");
        assertThat(body.get("code_verifier")).isEqualTo("qvCFoo3tqB-89lYOFjX2ZAMalkKgW_KIcc1tN3hmx3ygOHyYbWT9hKU7rhky6ivB-33exlhyyHHeSJtuVfOobg");
        assertThat(body.get("grant_type")).isEqualTo("authorization_code");
        assertThat(body.get("code")).isEqualTo("roxwkG0TtooR2vzA6z0MT9xyJSQ");

    }

    @Test
    public void testLogout() throws InterruptedException, ExecutionException, AuthenticationRequiredException,IOException, ApiException {
        testHappyPath();
        //Access Token Revoke
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));
        //ID Token endsession
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_NO_CONTENT));

        FRUser.getCurrentUser().logout();
        assertNull(FRUser.getCurrentUser());

        assertFalse(Config.getInstance().getSessionManager().hasSession());

        RecordedRequest revoke1 = server.takeRequest(); //Post to oauth2/realms/root/token/revoke
        RecordedRequest revoke2 = server.takeRequest(); //Post to /endSession

        //revoke Refresh Token and SSO Token are performed async
        RecordedRequest refreshTokenRevoke = findRequest("/oauth2/realms/root/token/revoke", revoke1, revoke2);
        RecordedRequest endSession = findRequest("/oauth2/realms/root/connect/endSession", revoke1, revoke2);

        assertThat(refreshTokenRevoke).isNotNull();
        assertThat(Uri.parse(endSession.getPath()).getQueryParameter("id_token_hint")).isNotNull();

    }

    @Test
    public void testRevokeTokenFailed() throws InterruptedException, ExecutionException, AuthenticationRequiredException, IOException, ApiException {
        testHappyPath();
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .addHeader("Content-Type", "application/json")
                .setBody("{\n" +
                        "    \"error_description\": \"Client authentication failed\",\n" +
                        "    \"error\": \"invalid_client\"\n" +
                        "}"));
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_NO_CONTENT));
        FRUser.getCurrentUser().logout();
        assertNull(FRUser.getCurrentUser());

        assertFalse(Config.getInstance().getSessionManager().hasSession());

        RecordedRequest revoke1 = server.takeRequest(); //Post to oauth2/realms/root/token/revoke
        RecordedRequest revoke2 = server.takeRequest(); //Post to /endSession

        //revoke Refresh Token and SSO Token are performed async
        RecordedRequest refreshTokenRevoke = findRequest("/oauth2/realms/root/token/revoke", revoke1, revoke2);
        RecordedRequest endSession = findRequest("/oauth2/realms/root/connect/endSession", revoke1, revoke2);

        assertThat(refreshTokenRevoke).isNotNull();

        //Make sure we still invoke the endSession
        assertThat(Uri.parse(endSession.getPath()).getQueryParameter("id_token_hint")).isNotNull();

    }

    //It is running with JVM, no browser is expected
    @Test(expected = ActivityNotFoundException.class)
    public void testAppAuthConfigurer() throws InterruptedException, ExecutionException, AuthenticationRequiredException {
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        FRListenerFuture<FRUser> future = new FRListenerFuture<>();
        AtomicInteger invoked = new AtomicInteger();
        FRUser.browser().
                appAuthConfigurer()
                .authorizationRequest(builder -> {
                    //Test populated data
                    AuthorizationRequest request = builder.build();
                    assertThat(request.clientId).isEqualTo(oAuth2Client.getClientId());
                    assertThat(request.redirectUri).isEqualTo(Uri.parse(oAuth2Client.getRedirectUri()));
                    assertThat(request.scope).isEqualTo(oAuth2Client.getScope());
                    assertThat(request.responseType).isEqualTo(oAuth2Client.getResponseType());
                    invoked.getAndIncrement();
                })
                .appAuthConfiguration(builder -> {
                    assertThat(builder).isNotNull();
                    invoked.getAndIncrement();
                })
                .customTabsIntent(builder -> {
                    assertThat(builder).isNotNull();
                    invoked.getAndIncrement();
                })
                .authorizationServiceConfiguration(() -> {
                    invoked.getAndIncrement();
                    try {
                        return new AuthorizationServiceConfiguration(
                                Uri.parse(oAuth2Client.getAuthorizeUrl().toString()),
                                Uri.parse(oAuth2Client.getTokenUrl().toString()));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .done().login(fragmentActivity, future);

        assertThat(invoked.get()).isEqualTo(4);

    }


    @Test
    public void testOperationCancel() throws InterruptedException {
        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        FRListenerFuture<FRUser> future = new FRListenerFuture<>();
        FRUser.browser()
                .failedOnNoBrowserFound(false)
                .login(fragmentActivity, future);
        AppAuthFragment appAuthFragment = getAppAuthFragment(fragmentActivity);
        Intent intent = new Intent();
        intent.putExtra(AuthorizationException.EXTRA_EXCEPTION, "{\"type\":0,\"code\":1,\"errorDescription\":\"User cancelled flow\"}");
        appAuthFragment.onActivityResult(AppAuthFragment.AUTH_REQUEST_CODE, Activity.RESULT_CANCELED, intent);
        try {
            future.get();
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(BrowserAuthenticationException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("{\"type\":0,\"code\":1,\"errorDescription\":\"User cancelled flow\"}");
        }
    }

    @Test(expected = AlreadyAuthenticatedException.class)
    public void testUserAlreadyAuthenticate() throws Throwable {
        testHappyPath();
        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        FRListenerFuture<FRUser> future = new FRListenerFuture<>();
        FRUser.browser().login(fragmentActivity, future);
        try {
            future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    private Map<String, String> parse(String encoded) {
        String[] body = encoded.split("&");
        Map<String, String> result = new HashMap<>();
        for (String s : body) {
            String[] value = s.split("=");
            result.put(value[0], value[1]);
        }
        return result;
    }


    @Test
    public void testInvalidScope() throws InterruptedException {
        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        FRListenerFuture<FRUser> future = new FRListenerFuture<>();
        FRUser.browser()
                .failedOnNoBrowserFound(false)
                .login(fragmentActivity, future);
        AppAuthFragment appAuthFragment = getAppAuthFragment(fragmentActivity);
        Intent intent = new Intent();
        intent.putExtra(AuthorizationException.EXTRA_EXCEPTION, INVALID_SCOPE);
        appAuthFragment.onActivityResult(AppAuthFragment.AUTH_REQUEST_CODE, Activity.RESULT_OK, intent);
        try {
            future.get();
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(BrowserAuthenticationException.class);
            assertThat((e.getCause()).getMessage()).isEqualTo(INVALID_SCOPE);
        }
    }

    @Test
    public void testRequestInterceptor() throws InterruptedException, ExecutionException, AuthenticationRequiredException, IOException, ApiException {

        final HashMap<String, Pair<Action, Integer>> result = new HashMap<>();
        RequestInterceptorRegistry.getInstance().register(request -> {
            String action = ((Action) request.tag()).getType();
            Pair<Action, Integer> pair = result.get(action);
            if (pair == null) {
                result.put(action, new Pair<>((Action) request.tag(), 1));
            } else {
                result.put(action, new Pair<>((Action) request.tag(), pair.second + 1));
            }
            return request;
        });

        testHappyPath();
        //Access Token Revoke
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .setBody("{}"));
        //ID Token endsession
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_NO_CONTENT));

        FRUser.getCurrentUser().logout();
        assertNull(FRUser.getCurrentUser());

        assertFalse(Config.getInstance().getSessionManager().hasSession());

        RecordedRequest revoke1 = server.takeRequest(); //Post to oauth2/realms/root/token/revoke
        RecordedRequest revoke2 = server.takeRequest(); //Post to /endSession

        Assertions.assertThat(result.get("END_SESSION").second).isEqualTo(1);

    }

    private RecordedRequest findRequest(String path, RecordedRequest... recordedRequests) {
        for (RecordedRequest r : recordedRequests) {
            if (r.getPath().startsWith(path)) {
                return r;
            }
        }
        throw new IllegalArgumentException();
    }



}
