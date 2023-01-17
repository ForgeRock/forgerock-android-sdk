/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class PersistentCookieTest extends BaseTest {

    private static final String DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest";

    @Before
    public void setupConfig() throws Exception {
        //Since the application context changed for each test, we cannot cache the storage in SecureCookieJar.
        OkHttpClientProvider.getInstance().clear();
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
    }

    @Test
    public void persistCookieHappyPathTest() throws InterruptedException, ExecutionException {

        server.enqueue(new MockResponse()
                .setResponseCode(HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
                .addHeader("Set-Cookie", "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
                .addHeader("Set-Cookie", "amlbcookie=01; Path=/; Domain=localhost")
                .setBody(getJson("/authTreeMockTest_Authenticate_success.json")));

        enqueue("/authTreeMockTest_Authenticate_success.json", HTTP_OK);

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
            }
        };

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        assertThat(nodeListenerFuture.get()).isInstanceOf(FRSession.class);
        assertThat(FRSession.getCurrentSession()).isNotNull();
        assertThat(FRSession.getCurrentSession().getSessionToken()).isNotNull();
        assertThat(FRSession.getCurrentSession().getSessionCookies()).hasSize(3);

        nodeListenerFuture.reset();
        FRSession.authenticate(context, "Example", nodeListenerFuture);
        nodeListenerFuture.get();

        server.takeRequest();
        RecordedRequest rr = server.takeRequest(); //Second request
        //The request should contains the received cookies
        assertThat(toMap(rr.getHeader("Cookie")).get("session-jwt")).isEqualTo("session-jwt-cookie");
        assertThat(toMap(rr.getHeader("Cookie")).get("iPlanetDirectoryPro")).isEqualTo("iPlanetDirectoryProCookie");

    }

    @Test
    public void replaceCookie() throws InterruptedException, ExecutionException {

        server.enqueue(new MockResponse()
                .setResponseCode(HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
                .addHeader("Set-Cookie", "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
                .addHeader("Set-Cookie", "amlbcookie=01; Path=/; Domain=localhost")
                .setBody(getJson("/authTreeMockTest_Authenticate_success.json")));

        server.enqueue(new MockResponse()
                .setResponseCode(HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
                .addHeader("Set-Cookie", "session-jwt=session-jwt-cookie-new; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
                .addHeader("Set-Cookie", "amlbcookie=01; Path=/; Domain=localhost")
                .setBody(getJson("/authTreeMockTest_Authenticate_success.json")));

        enqueue("/authTreeMockTest_Authenticate_success.json", HTTP_OK);


        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
            }
        };

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        assertThat(nodeListenerFuture.get()).isInstanceOf(FRSession.class);
        assertThat(FRSession.getCurrentSession()).isNotNull();
        assertThat(FRSession.getCurrentSession().getSessionToken()).isNotNull();

        //Second request with new cookie value
        nodeListenerFuture.reset();
        FRSession.authenticate(context, "Example", nodeListenerFuture);
        nodeListenerFuture.get();

        //Third request to check if the request has updated cookie
        nodeListenerFuture.reset();
        FRSession.authenticate(context, "Example", nodeListenerFuture);
        nodeListenerFuture.get();

        server.takeRequest(); //first request
        server.takeRequest(); //second request
        RecordedRequest rr = server.takeRequest(); //Third request
        //The request should contains the received cookies
        assertThat(toMap(rr.getHeader("Cookie")).get("session-jwt")).isEqualTo("session-jwt-cookie-new");
        assertThat(toMap(rr.getHeader("Cookie")).get("iPlanetDirectoryPro")).isEqualTo("iPlanetDirectoryProCookie");

    }

    @Test
    public void appendCookie() throws InterruptedException, ExecutionException {

        server.enqueue(new MockResponse()
                .setResponseCode(HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
                .setBody(getJson("/authTreeMockTest_Authenticate_success.json")));

        server.enqueue(new MockResponse()
                .setResponseCode(HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "session-jwt=session-jwt-cookie-new; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
                .setBody(getJson("/authTreeMockTest_Authenticate_success.json")));

        enqueue("/authTreeMockTest_Authenticate_success.json", HTTP_OK);


        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
            }
        };

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        assertThat(nodeListenerFuture.get()).isInstanceOf(FRSession.class);
        assertThat(FRSession.getCurrentSession()).isNotNull();
        assertThat(FRSession.getCurrentSession().getSessionToken()).isNotNull();

        //Second request with new cookie
        nodeListenerFuture.reset();
        FRSession.authenticate(context, "Example", nodeListenerFuture);
        nodeListenerFuture.get();

        //Third request to check if the request has updated cookie
        nodeListenerFuture.reset();
        FRSession.authenticate(context, "Example", nodeListenerFuture);
        nodeListenerFuture.get();

        server.takeRequest(); //first request
        server.takeRequest(); //second request
        RecordedRequest rr = server.takeRequest(); //Third request
        //The request should contains the received cookies
        assertThat(toMap(rr.getHeader("Cookie")).get("session-jwt")).isEqualTo("session-jwt-cookie-new");
        assertThat(toMap(rr.getHeader("Cookie")).get("iPlanetDirectoryPro")).isEqualTo("iPlanetDirectoryProCookie");
        assertThat(Config.getInstance().getSingleSignOnManager().getCookies()).hasSize(2);

    }


    @Test
    public void expiredCookie() throws InterruptedException, ExecutionException {

        server.enqueue(new MockResponse()
                .setResponseCode(HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
                .addHeader("Set-Cookie", "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 1999 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
                .setBody(getJson("/authTreeMockTest_Authenticate_success.json")));

        enqueue("/authTreeMockTest_Authenticate_success.json", HTTP_OK);


        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
            }
        };

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        assertThat(nodeListenerFuture.get()).isInstanceOf(FRSession.class);
        assertThat(FRSession.getCurrentSession()).isNotNull();
        assertThat(FRSession.getCurrentSession().getSessionToken()).isNotNull();

        nodeListenerFuture.reset();
        FRSession.authenticate(context, "Example", nodeListenerFuture);
        nodeListenerFuture.get();

        server.takeRequest();
        RecordedRequest rr = server.takeRequest(); //Second request
        //The request should contains the received cookies
        assertThat(toMap(rr.getHeader("Cookie")).get("session-jwt")).isNull();
        assertThat(toMap(rr.getHeader("Cookie")).get("iPlanetDirectoryPro")).isEqualTo("iPlanetDirectoryProCookie");
        assertThat(Config.getInstance().getSingleSignOnManager().getCookies()).hasSize(1);

    }

    @Test
    public void secureCookie() throws InterruptedException, ExecutionException {

        server.enqueue(new MockResponse()
                .setResponseCode(HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
                .addHeader("Set-Cookie", "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2300 02:53:31 GMT; Path=/; Domain=localhost; Secure; HttpOnly")
                .setBody(getJson("/authTreeMockTest_Authenticate_success.json")));

        enqueue("/authTreeMockTest_Authenticate_success.json", HTTP_OK);


        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
            }
        };

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        assertThat(nodeListenerFuture.get()).isInstanceOf(FRSession.class);
        assertThat(FRSession.getCurrentSession()).isNotNull();
        assertThat(FRSession.getCurrentSession().getSessionToken()).isNotNull();

        nodeListenerFuture.reset();
        FRSession.authenticate(context, "Example", nodeListenerFuture);
        nodeListenerFuture.get();

        server.takeRequest();
        RecordedRequest rr = server.takeRequest(); //Second request
        //The request should contains the received cookies but not the one with secure, because we are using http, not https
        assertThat(toMap(rr.getHeader("Cookie")).get("session-jwt")).isNull();
        assertThat(toMap(rr.getHeader("Cookie")).get("iPlanetDirectoryPro")).isEqualTo("iPlanetDirectoryProCookie");
        //The cookie is stored, but not sent
        assertThat(Config.getInstance().getSingleSignOnManager().getCookies()).hasSize(2);
    }



    @Test
    public void cookieCache() throws InterruptedException, ExecutionException {

        Config.getInstance().setCookieJar(SecureCookieJar.builder()
                .context(context)
                .cacheIntervalMillis(1000L)
                .build());
        server.enqueue(new MockResponse()
                .setResponseCode(HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
                .addHeader("Set-Cookie", "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
                .setBody(getJson("/authTreeMockTest_Authenticate_success.json")));

        enqueue("/authTreeMockTest_Authenticate_success.json", HTTP_OK);

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
            }
        };

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        assertThat(nodeListenerFuture.get()).isInstanceOf(FRSession.class);

        //Second request with delete the cookies
        Config.getInstance().getSingleSignOnManager().clear();
        nodeListenerFuture.reset();
        FRSession.authenticate(context, "Example", nodeListenerFuture);
        nodeListenerFuture.get();

        server.takeRequest(); //first request
        RecordedRequest rr = server.takeRequest(); //Second request
        //The request should contains the received cookies
        assertThat(toMap(rr.getHeader("Cookie")).get("session-jwt")).isEqualTo("session-jwt-cookie");
        assertThat(toMap(rr.getHeader("Cookie")).get("iPlanetDirectoryPro")).isEqualTo("iPlanetDirectoryProCookie");

    }

    @Test
    public void cookieCacheRemovedWithLogout() throws InterruptedException, ExecutionException {

        server.enqueue(new MockResponse()
                .setResponseCode(HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=localhost")
                .addHeader("Set-Cookie", "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
                .setBody(getJson("/authTreeMockTest_Authenticate_success.json")));

        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);

        enqueue("/authTreeMockTest_Authenticate_success.json", HTTP_OK);

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
            }
        };

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        assertThat(nodeListenerFuture.get()).isInstanceOf(FRSession.class);

        FRSession.getCurrentSession().logout();

        //Second request with delete the cookies
        nodeListenerFuture.reset();
        FRSession.authenticate(context, "Example", nodeListenerFuture);
        nodeListenerFuture.get();

        server.takeRequest(); //first request
        server.takeRequest(); //logout request
        RecordedRequest rr = server.takeRequest(); //Second request
        //The request should contains the received cookies
        assertThat(rr.getHeader("Cookie")).isNull();

    }

    private Map<String, String> toMap(String cookieStr) {
        if (cookieStr == null) {
            return null;
        }
        String[] cookies = cookieStr.split(";");
        Map<String, String> result = new HashMap<>();
        for (String cookie: cookies ) {
            String[] pair = cookie.split("=");
            result.put(pair[0].trim(), pair[1].trim());
        }
        return result;
    }

    @Test
    public void persistWithDifferentDomain() throws InterruptedException, ExecutionException {

        server.enqueue(new MockResponse()
                .setResponseCode(HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "iPlanetDirectoryPro=iPlanetDirectoryProCookie; Path=/; Domain=test")
                .addHeader("Set-Cookie", "session-jwt=session-jwt-cookie; Expires=Tue, 21 Jan 2220 02:53:31 GMT; Path=/; Domain=localhost; HttpOnly")
                .setBody(getJson("/authTreeMockTest_Authenticate_success.json")));

        enqueue("/authTreeMockTest_Authenticate_success.json", HTTP_OK);

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
            }
        };

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        assertThat(nodeListenerFuture.get()).isInstanceOf(FRSession.class);
        assertThat(FRSession.getCurrentSession()).isNotNull();
        assertThat(FRSession.getCurrentSession().getSessionToken()).isNotNull();

        nodeListenerFuture.reset();
        FRSession.authenticate(context, "Example", nodeListenerFuture);
        nodeListenerFuture.get();

        server.takeRequest();
        RecordedRequest rr = server.takeRequest(); //Second request

        assertThat(toMap(rr.getHeader("Cookie")).get("iPlanetDirectoryPro")).isNull(); //Not the same domain
        assertThat(toMap(rr.getHeader("Cookie")).get("session-jwt")).isEqualTo("session-jwt-cookie");

    }
}
