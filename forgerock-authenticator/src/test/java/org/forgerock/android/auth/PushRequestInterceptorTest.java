/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static junit.framework.Assert.assertEquals;

import static org.forgerock.android.auth.Action.PUSH_AUTHENTICATE;
import static org.forgerock.android.auth.Action.PUSH_REGISTER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import android.net.Uri;

import androidx.annotation.NonNull;

@RunWith(RobolectricTestRunner.class)
public class PushRequestInterceptorTest extends FRABaseTest {
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
    public void testPushRegistration() throws Exception {
        RequestInterceptorRegistry.getInstance().register(new PushRequestInterceptor());

        server.enqueue(new MockResponse());
        HttpUrl baseUrl = server.url("/");

        PushResponder.getInstance(storageClient).registration(baseUrl.toString(), "testCookie", "b3uYLkQ7dRPjBaIzV0t/aijoXRgMq+NP5AwVAvRfa/E=",
                "testMessageId", new HashMap<String, Object>(), pushListenerFuture);
        RecordedRequest request = server.takeRequest();

        assertEquals("PUSH_REGISTER", request.getHeader("testHeader"));
        assertEquals("PUSH_REGISTER", request.getRequestUrl().queryParameter("testParameter"));
    }

    @Test
    public void testPushAuthentication() throws Exception {
        RequestInterceptorRegistry.getInstance().register(new PushRequestInterceptor());
        server.enqueue(new MockResponse());

        PushResponder.getInstance(storageClient).authentication(newPushNotification(), true, pushListenerFuture);
        RecordedRequest request = server.takeRequest();
        pushListenerFuture.get();

        assertEquals("PUSH_AUTHENTICATE", request.getHeader("testHeader"));
        assertEquals("PUSH_AUTHENTICATE", request.getRequestUrl().queryParameter("testParameter"));
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

    private class PushRequestInterceptor implements FRRequestInterceptor<Action> {
        @NonNull
        @Override
        public Request intercept(@NonNull Request request, Action tag) {
            if (tag.getType().equals(PUSH_REGISTER)) {
                return request.newBuilder()
                        // Add query parameter:
                        .url(Uri.parse(request.url().toString())
                                .buildUpon()
                                .appendQueryParameter("testParameter", "PUSH_REGISTER").toString())

                        // Add additional header:
                        .addHeader("testHeader", "PUSH_REGISTER")

                        // Construct the updated request:
                        .build();
            } else if (tag.getType().equals(PUSH_AUTHENTICATE)) {
                return request.newBuilder()
                        // Add query parameter:
                        .url(Uri.parse(request.url().toString())
                                .buildUpon()
                                .appendQueryParameter("testParameter", "PUSH_AUTHENTICATE").toString())

                        // Add additional header:
                        .addHeader("testHeader", "PUSH_AUTHENTICATE")

                        // Construct the updated request:
                        .build();
            }
            return request;
        }
    }
}


