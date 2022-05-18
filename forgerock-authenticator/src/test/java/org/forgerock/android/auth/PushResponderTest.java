/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.ChallengeResponseException;
import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.exception.PushMechanismException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

        PushResponder.getInstance(storageClient).registration(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
        RecordedRequest request = server.takeRequest();

        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals("testCookie", request.getHeader("Cookie"));
    }

    @Test
    public void testReplyRegistrationMessageServerConnectionFailure() {
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));
        HttpUrl baseUrl = server.url("/");

        PushResponder.getInstance(storageClient).registration(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
        try {
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof PushMechanismException);
            assertTrue(e.getLocalizedMessage().contains("Communication with server returned 404 code."));
        }
    }

    @Test
    public void testReplyRegistrationMessageNetworkFailure() {
        MockResponse response = new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START);
        server.enqueue(response);
        HttpUrl baseUrl = server.url("/");

        try {
            PushResponder.getInstance(storageClient).registration(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
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

        PushResponder.getInstance(storageClient).authentication(newPushNotification(), true, pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        pushListenerFuture.get();

        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals(AMLB_COOKIE, request.getHeader("Cookie"));
    }

    @Test
    public void testReplyAuthenticationMessageServerConnectionFailure() {
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));

        try {
            PushResponder.getInstance(storageClient).authentication(newPushNotification(), true, pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof PushMechanismException);
            assertTrue(e.getLocalizedMessage().contains("Communication with server returned 404 code."));
        }
    }

    @Test
    public void testReplyAuthenticationMessageNetworkFailure() {
        MockResponse response = new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START);
        server.enqueue(response);

        try {
            PushResponder.getInstance(storageClient).authentication(newPushNotification(), true, pushListenerFuture);
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
            PushResponder.getInstance(storageClient).registration(baseUrl.toString(), "testCookie", "",
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
            PushResponder.getInstance(storageClient).registration(baseUrl.toString(), "testCookie", "dGVzdHNlY3JldA==",
                    "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof PushMechanismException);
            assertTrue(e.getLocalizedMessage().contains("The secret length must be at least 256 bits"));
        }
    }

    @Test
    public void testShouldGenerateChallengeResponseCorrectly() throws ChallengeResponseException {
        String base64Secret = "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=";
        String base64Challenge = "9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg=";

        String response = PushResponder.getInstance(storageClient).generateChallengeResponse(base64Secret, base64Challenge);

        assertEquals(response, "Df02AwA3Ra+sTGkL5+QvkEtN3eLdZiFmL5nxAV1m0k8=");
    }

    @Test
    public void testShouldFailTOGenerateChallengeResponse() {
        String base64Secret = "";
        String base64Challenge = "9giiBAdUHjqpo0XE4YdZ7pRlv0hrQYwDz8Z1wwLLbkg=";

        String response = null;

        try {
            response = PushResponder.getInstance(storageClient).generateChallengeResponse(base64Secret, base64Challenge);
            fail("Should throw ChallengeResponseException");
        } catch (Exception e) {
            assertNull(response);
            assertTrue(e instanceof ChallengeResponseException);
        }
    }

    @Test
    public void testShouldSignJWTCorrectly() throws Exception {
        final String base64Secret = "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=";
        server.enqueue(new MockResponse());
        HttpUrl baseUrl = server.url("/");

        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", "testFcmToken");
        payload.put("deviceType", "android");
        payload.put("communicationType", "gcm");
        payload.put("mechanismUid", "testMechanismUid");
        payload.put("response", "Df02AwA3Ra+sTGkL5+QvkEtN3eLdZiFmL5nxAV1m0k8=");

        PushResponder.getInstance(storageClient).registration(baseUrl.toString(), "testCookie", base64Secret,
                "testMessageId", payload, pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();

        assertEquals("resource=1.0, protocol=1.0", request.getHeader("Accept-API-Version"));
        assertEquals("testCookie", request.getHeader("Cookie"));

        // Extract the JWT from the request body
        JSONObject json = new JSONObject(body);
        String jwt = json.getString("jwt");
        String[] splitJwt = jwt.split("\\.");
        String jwtHeader = splitJwt[0];
        String jwtPayload = splitJwt[1];
        String jwtSignature = splitJwt[2];

        // Calculate signature...
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        byte[] secretBytes = Base64.getDecoder().decode(base64Secret);
        SecretKeySpec secret_key = new SecretKeySpec(secretBytes, "HmacSHA256");
        sha256_HMAC.init(secret_key);

        String hash = new String(Base64.getEncoder().withoutPadding().encode(sha256_HMAC.doFinal((jwtHeader + "." + jwtPayload).getBytes())));

        // Verify that the signature is correct.
        assertEquals(hash, jwtSignature);
    }

    private PushNotification newPushNotification() throws InvalidNotificationException, MechanismCreationException {
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

    private PushMechanism newPushMechanism() throws MechanismCreationException {
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
