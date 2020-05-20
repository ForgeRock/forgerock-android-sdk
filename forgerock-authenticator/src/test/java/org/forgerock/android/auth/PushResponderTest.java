/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.ChallengeResponseException;
import org.forgerock.android.auth.exception.PushMechanismException;
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

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class PushResponderTest extends FRABaseTest {
    private MockWebServer server;
    private FRAListenerFuture pushListenerFuture;
    private DefaultStorageClient storageClient;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        storageClient = mock(DefaultStorageClient.class);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);

        pushListenerFuture = new FRAListenerFuture<Integer>();
    }

    @After
    public void cleanUp() throws Exception {
        server.shutdown();
        PushResponder.reset();
    }

    @Test
    public void testShouldFailToGetInstanceNotInitialized() {
        PushResponder pushResponder = null;

        try {
            pushResponder = PushResponder.getInstance();
            fail("Should throw IllegalStateException");
        } catch (Exception e) {
            assertNull(pushResponder);
            assertTrue(e instanceof IllegalStateException);
        }
    }

    @Test
    public void testShouldReplyRegistrationMessageCorrectly() throws Exception {
        server.enqueue(new MockResponse());
        HttpUrl baseUrl = server.url("/");

        PushResponder.init(storageClient).registration(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(responseCode, HTTP_OK);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals("testCookie", request.getHeader("Cookie"));
    }

    @Test
    public void testReplyRegistrationMessageServerConnectionFailure() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));
        HttpUrl baseUrl = server.url("/");

        PushResponder.init(storageClient).registration(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(responseCode, HTTP_NOT_FOUND);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals("testCookie", request.getHeader("Cookie"));
    }

    @Test
    public void testReplyRegistrationMessageNetworkFailure() {
        MockResponse response = new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START);
        server.enqueue(response);
        HttpUrl baseUrl = server.url("/");

        try {
            PushResponder.init(storageClient).registration(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                    "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof PushMechanismException);
            assertTrue(e.getLocalizedMessage().contains("Network error while processing the Push Registration request"));
        }
    }

    @Test
    public void testShouldReplyAuthenticationMessageCorrectly() throws Exception {
        server.enqueue(new MockResponse());

        PushResponder.init(storageClient).authentication(newPushNotification(), true, pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(responseCode, HTTP_OK);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals(AMLB_COOKIE, request.getHeader("Cookie"));
    }

    @Test
    public void testReplyAuthenticationMessageServerConnectionFailure() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));

        PushResponder.init(storageClient).authentication(newPushNotification(), true, pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(responseCode, HTTP_NOT_FOUND);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals(AMLB_COOKIE, request.getHeader("Cookie"));
    }

    @Test
    public void testReplyAuthenticationMessageNetworkFailure() {
        MockResponse response = new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START);
        server.enqueue(response);

        try {
            PushResponder.init(storageClient).authentication(newPushNotification(), true, pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof PushMechanismException);
            assertTrue(e.getLocalizedMessage().contains("Network error while processing the Push Authentication request"));
        }
    }

    @Test
    public void testShouldRejectEmptySecret() {
        server.enqueue(new MockResponse());
        HttpUrl baseUrl = server.url("/");

        try {
            PushResponder.init(storageClient).registration(baseUrl.toString(), "testCookie", "",
                "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof PushMechanismException);
            assertTrue(e.getLocalizedMessage().contains("Passed empty secret"));
        }
    }

    @Test
    public void testShouldRejectInvalidSecret() {
        server.enqueue(new MockResponse());
        HttpUrl baseUrl = server.url("/");

        try {
            PushResponder.init(storageClient).registration(baseUrl.toString(), "testCookie", "dGVzdHNlY3JldA==",
                    "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof PushMechanismException);
            assertTrue(e.getLocalizedMessage().contains("Error signing JWT data. Secret malformed or invalid"));
        }
    }

    @Test
    public void testShouldGenerateChallengeResponseCorrectly() throws ChallengeResponseException {
        String base64Secret = "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=";
        String base64Challenge = "9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg=";

        String response = PushResponder.init(storageClient).generateChallengeResponse(base64Secret, base64Challenge);

        assertEquals(response, "Df02AwA3Ra+sTGkL5+QvkEtN3eLdZiFmL5nxAV1m0k8=");
    }

    @Test
    public void testShouldFailTOGenerateChallengeResponse() {
        String base64Secret = "";
        String base64Challenge = "9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg=";

        String response = null;

        try {
            response = PushResponder.init(storageClient).generateChallengeResponse(base64Secret, base64Challenge);
            fail("Should throw ChallengeResponseException");
        } catch (Exception e) {
            assertNull(response);
            assertTrue(e instanceof ChallengeResponseException);
        }
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

        PushResponder.init(storageClient).registration(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", payload, pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(responseCode, HTTP_OK);
        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals("testCookie", request.getHeader("Cookie"));
        assertThat(body, Matchers.containsString("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJkZXZpY2VUeXBlIjoiYW5kcm9pZCIsIm1lY2hhbmlzbVVpZCI6InRlc3RNZWNoYW5pc21VaWQiLCJyZXNwb25zZSI6IkRmMDJBd0EzUmErc1RHa0w1K1F2a0V0TjNlTGRaaUZtTDVueEFWMW0wazg9IiwiY29tbXVuaWNhdGlvblR5cGUiOiJnY20iLCJkZXZpY2VJZCI6InRlc3RGY21Ub2tlbiJ9.UimglbtcwK6vD0mYZW_B3Yge6chPR--5mPmyHB0maas"));
    }

    private PushNotification newPushNotification() {
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

        pushNotification.setPushMechanism(newPushMechanism());

        return pushNotification;
    }

    private PushMechanism newPushMechanism() {
        HttpUrl baseUrl = server.url("/");
        PushMechanism push = PushMechanism.builder()
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
