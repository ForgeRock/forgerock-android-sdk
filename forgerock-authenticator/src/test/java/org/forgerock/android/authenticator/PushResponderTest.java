/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import com.nimbusds.jose.JOSEException;

import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class PushResponderTest {
    private MockWebServer server;

    @Before
    public void setUp() {
        server = new MockWebServer();

    }

    @After
    public void cleanUp() throws Exception {
        server.shutdown();
    }

    @Test
    public void testShouldSendMessageCorrectly() throws Exception {
        server.enqueue(new MockResponse());
        server.start();

        HttpUrl baseUrl = server.url("/");

        int responseCode = PushResponder.respond(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", new HashMap<String, Object>());

        RecordedRequest request = server.takeRequest();

        assertEquals(responseCode, HTTP_OK);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals("testCookie", request.getHeader("Cookie"));
    }

    @Test
    public void testSendMessageFailure() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));
        server.start();

        HttpUrl baseUrl = server.url("/");

        int responseCode = PushResponder.respond(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", new HashMap<String, Object>());

        RecordedRequest request = server.takeRequest();

        assertEquals(responseCode, HTTP_NOT_FOUND);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals("testCookie", request.getHeader("Cookie"));
    }

    @Test
    public void testShouldRejectEmptySecret() {

        try {
            PushResponder.respond("http://example.com", "testCookie", "",
                    "testMessageId", new HashMap<String, Object>());
        } catch (IOException | JSONException | JOSEException e) {
            assertEquals(e.getClass(), IOException.class);
            assertEquals(e.getLocalizedMessage(), "Passed empty secret");
        }
    }

    @Test
    public void testShouldRejectInvalidSecret() {

        try {
            PushResponder.respond("http://example.com", "testCookie", "dGVzdHNlY3JldA==",
                    "testMessageId", new HashMap<String, Object>());
        } catch (IOException | JSONException | JOSEException e) {
            assertEquals(e.getClass(), IOException.class);
            assertEquals(e.getLocalizedMessage(), "Invalid secret!");
        }
    }

    @Test
    public void testShouldGenerateChallengeResponseCorrectly() {
        String base64Secret = "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=";
        String base64Challenge = "9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg=";

        String response = PushResponder.generateChallengeResponse(base64Secret, base64Challenge);

        assertEquals(response, "Df02AwA3Ra+sTGkL5+QvkEtN3eLdZiFmL5nxAV1m0k8=");
    }

    @Test
    public void testShouldSignJWTCorrectly() throws Exception {
        server.enqueue(new MockResponse());
        server.start();

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", "testFcmToken");
        payload.put("deviceType", "android");
        payload.put("communicationType", "gcm");
        payload.put("mechanismUid", "testMechanismUid");
        payload.put("response", "Df02AwA3Ra+sTGkL5+QvkEtN3eLdZiFmL5nxAV1m0k8=");

        HttpUrl baseUrl = server.url("/");

        int responseCode = PushResponder.respond(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", payload);

        RecordedRequest request = server.takeRequest();

        String body = request.getBody().readUtf8();

        assertEquals(responseCode, HTTP_OK);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals("testCookie", request.getHeader("Cookie"));
        assertThat(body, Matchers.containsString("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJkZXZpY2VUeXBlIjoiYW5kcm9pZCIsIm1lY2hhbmlzbVVpZCI6InRlc3RNZWNoYW5pc21VaWQiLCJyZXNwb25zZSI6IkRmMDJBd0EzUmErc1RHa0w1K1F2a0V0TjNlTGRaaUZtTDVueEFWMW0wazg9IiwiY29tbXVuaWNhdGlvblR5cGUiOiJnY20iLCJkZXZpY2VJZCI6InRlc3RGY21Ub2tlbiJ9.UimglbtcwK6vD0mYZW_B3Yge6chPR--5mPmyHB0maas"));
    }

}
