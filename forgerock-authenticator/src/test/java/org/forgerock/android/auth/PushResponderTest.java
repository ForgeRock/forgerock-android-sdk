/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import com.nimbusds.jose.JOSEException;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PushResponderTest extends FRABaseTest {
    private MockWebServer server;
    private PushResponder pushResponder;
    private FRAListenerFuture pushListenerFuture;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        pushResponder = PushResponder.getInstance();

        pushListenerFuture = new FRAListenerFuture<Integer>();
    }

    @After
    public void cleanUp() throws Exception {
        server.shutdown();
    }

    @Test
    public void testShouldReplyRegistrationMessageCorrectly() throws Exception {
        server.enqueue(new MockResponse());
        HttpUrl baseUrl = server.url("/");

        pushResponder.registration(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(responseCode, HTTP_OK);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals("testCookie", request.getHeader("Cookie"));
    }

    @Test
    public void testReplyRegistrationMessageFailure() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));
        HttpUrl baseUrl = server.url("/");

        pushResponder.registration(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(responseCode, HTTP_NOT_FOUND);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals("testCookie", request.getHeader("Cookie"));
    }

    @Test
    public void testShouldReplyAuthenticationMessageCorrectly() throws Exception {
        server.enqueue(new MockResponse());

        pushResponder.authentication(createPushNotification(), true, pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(responseCode, HTTP_OK);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals(AMLB_COOKIE, request.getHeader("Cookie"));
    }

    @Test
    public void testReplyAuthenticationMessageFailure() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));

        pushResponder.authentication(createPushNotification(), true, pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(responseCode, HTTP_NOT_FOUND);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals(AMLB_COOKIE, request.getHeader("Cookie"));
    }


    @Test
    public void testShouldRejectEmptySecret() throws Exception {
        server.enqueue(new MockResponse());
        HttpUrl baseUrl = server.url("/");

        try {
            pushResponder.registration(baseUrl.toString(), "testCookie", "",
                "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw IOException");
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage().contains("Passed empty secret"));
        }
    }

    @Test
    public void testShouldRejectInvalidSecret() {
        server.enqueue(new MockResponse());
        HttpUrl baseUrl = server.url("/");

        try {
            pushResponder.registration(baseUrl.toString(), "testCookie", "dGVzdHNlY3JldA==",
                    "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw IOException");
        } catch (Exception e) {
            assertTrue(e.getLocalizedMessage().contains("Invalid secret!"));
        }
    }

    @Test
    public void testShouldGenerateChallengeResponseCorrectly() {
        String base64Secret = "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=";
        String base64Challenge = "9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg=";

        String response = pushResponder.generateChallengeResponse(base64Secret, base64Challenge);

        assertEquals(response, "Df02AwA3Ra+sTGkL5+QvkEtN3eLdZiFmL5nxAV1m0k8=");
    }

    @Test
    public void testShouldSignJWTCorrectly() throws Exception {
        server.enqueue(new MockResponse());
        HttpUrl baseUrl = server.url("/");

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", "testFcmToken");
        payload.put("deviceType", "android");
        payload.put("communicationType", "gcm");
        payload.put("mechanismUid", "testMechanismUid");
        payload.put("response", "Df02AwA3Ra+sTGkL5+QvkEtN3eLdZiFmL5nxAV1m0k8=");

        pushResponder.registration(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", payload, pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(responseCode, HTTP_OK);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals("testCookie", request.getHeader("Cookie"));
        assertThat(body, Matchers.containsString("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJkZXZpY2VUeXBlIjoiYW5kcm9pZCIsIm1lY2hhbmlzbVVpZCI6InRlc3RNZWNoYW5pc21VaWQiLCJyZXNwb25zZSI6IkRmMDJBd0EzUmErc1RHa0w1K1F2a0V0TjNlTGRaaUZtTDVueEFWMW0wazg9IiwiY29tbXVuaWNhdGlvblR5cGUiOiJnY20iLCJkZXZpY2VJZCI6InRlc3RGY21Ub2tlbiJ9.UimglbtcwK6vD0mYZW_B3Yge6chPR--5mPmyHB0maas"));
    }

    private PushNotification createPushNotification() {
        Calendar time = Calendar.getInstance();
        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(time)
                .setTimeExpired(time)
                .setApproved(false)
                .setPending(true)
                .setTtl(TTL)
                .build();

        pushNotification.setPushMechanism(createPushMechanism());

        return pushNotification;
    }

    private Push createPushMechanism() {
        HttpUrl baseUrl = server.url("/");
        Push push = Push.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(baseUrl.toString())
                .setRegistrationEndpoint(baseUrl.toString())
                .setSecret("b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=")
                .build();
        return push;
    }

}
