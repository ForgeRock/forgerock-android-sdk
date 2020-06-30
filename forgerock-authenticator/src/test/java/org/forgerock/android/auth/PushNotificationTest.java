/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import com.google.firebase.messaging.RemoteMessage;

import org.forgerock.android.auth.exception.PushMechanismException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Calendar;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class PushNotificationTest extends FRABaseTest {

    private MockWebServer server;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void cleanUp() throws Exception {
        server.shutdown();
    }

    @Test
    public void testCreateNotificationSuccessfuly() {
        Calendar timeAdded = Calendar.getInstance();
        Calendar timeExpired = Calendar.getInstance();

        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTimeExpired(timeExpired)
                .setTtl(TTL)
                .build();

        assertEquals(pushNotification.getMechanismUID(), MECHANISM_UID);
        assertEquals(pushNotification.getMessageId(), MESSAGE_ID);
        assertEquals(pushNotification.getChallenge(), CHALLENGE);
        assertEquals(pushNotification.getAmlbCookie(), AMLB_COOKIE);
        assertEquals(pushNotification.getTtl(), TTL);
        assertEquals(pushNotification.getTimeAdded(), timeAdded);
        assertEquals(pushNotification.getTimeExpired(), timeExpired);
    }

    @Test
    public void testCreateNotificationWithOptionalParametersSuccessfuly() {
        Calendar timeAdded = Calendar.getInstance();
        Calendar timeExpired = Calendar.getInstance();
        long time = timeExpired.getTimeInMillis();
        timeExpired.setTimeInMillis(time+3000);
        boolean approved = false;
        boolean pending = true;

        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTimeExpired(timeExpired)
                .setTtl(TTL)
                .setApproved(approved)
                .setPending(pending)
                .build();

        assertEquals(pushNotification.getMechanismUID(), MECHANISM_UID);
        assertEquals(pushNotification.getMessageId(), MESSAGE_ID);
        assertEquals(pushNotification.getChallenge(), CHALLENGE);
        assertEquals(pushNotification.getAmlbCookie(), AMLB_COOKIE);
        assertEquals(pushNotification.getTtl(), TTL);
        assertEquals(pushNotification.getTimeAdded(), timeAdded);
        assertEquals(pushNotification.isApproved(), approved);
        assertEquals(pushNotification.isPending(), pending);
        assertEquals(pushNotification.isExpired(), false);
    }

    @Test
    public void testShouldBeEqualEquivalentNotification() {
        Calendar timeAdded = Calendar.getInstance();
        PushNotification pushNotification1 = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .build();
        PushNotification pushNotification2 = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .build();

        assertTrue(pushNotification1.matches(pushNotification2));
        assertTrue(pushNotification1.matches(pushNotification2));
        assertEquals(pushNotification1.hashCode(), pushNotification2.hashCode());
    }

    @Test
    public void testShouldNotBeEqualNotificationWithDifferentMechanismUID() {
        Calendar timeAdded = Calendar.getInstance();
        PushNotification pushNotification1 = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .build();
        PushNotification pushNotification2 = PushNotification.builder()
                .setMechanismUID(OTHER_MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .build();

        assertFalse(pushNotification1.equals(pushNotification2));
        assertFalse(pushNotification1.matches(pushNotification2));
    }

    @Test
    public void testShouldNotBeEqualNotificationWithDifferentTimeAdded() {
        Calendar timeAdded1 = Calendar.getInstance();

        long time = timeAdded1.getTimeInMillis();
        Calendar timeAdded2 = Calendar.getInstance();
        timeAdded2.setTimeInMillis(time+100);

        PushNotification pushNotification1 = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded1)
                .setTtl(TTL)
                .build();
        PushNotification pushNotification2 = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded2)
                .setTtl(TTL)
                .build();

        assertFalse(pushNotification1.equals(pushNotification2));
        assertFalse(pushNotification1.matches(pushNotification2));
    }

    @Test
    public void testShouldAcceptCorrectly() throws Exception {
        server.enqueue(new MockResponse());

        StorageClient storageClient = mock(DefaultStorageClient.class);
        PushResponder.getInstance(storageClient);

        HttpUrl baseUrl = server.url("/");
        PushMechanism push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();

        pushNotification.accept(pushListenerFuture);
        pushListenerFuture.get();

        assertTrue(pushNotification.isApproved());
        assertFalse(pushNotification.isPending());
    }

    @Test
    public void testAcceptFailureNetworkError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));

        StorageClient storageClient = mock(DefaultStorageClient.class);
        PushResponder.getInstance(storageClient);

        HttpUrl baseUrl = server.url("/");
        PushMechanism push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Void>();
        try{
            pushNotification.accept(pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof PushMechanismException);
            assertTrue(e.getLocalizedMessage().contains("Communication with server returned 404 code"));
            assertFalse(pushNotification.isApproved());
            assertTrue(pushNotification.isPending());
        }
    }

    @Test
    public void testAcceptFailureAlreadyApproved() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_OK));

        StorageClient storageClient = mock(DefaultStorageClient.class);
        PushResponder.getInstance(storageClient);

        HttpUrl baseUrl = server.url("/");
        PushMechanism push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);
        pushNotification.setPending(false);
        pushNotification.setApproved(true);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();
        try {
            pushNotification.accept(pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertTrue(pushNotification.isApproved());
            assertFalse(pushNotification.isPending());
            assertTrue(e.getCause() instanceof PushMechanismException);
            assertTrue(e.getLocalizedMessage().contains("PushNotification is not in a valid status to authenticate;"));
        }
    }

    @Test
    public void testShouldDenyCorrectly() throws Exception {
        server.enqueue(new MockResponse());

        StorageClient storageClient = mock(DefaultStorageClient.class);
        PushResponder.getInstance(storageClient);

        HttpUrl baseUrl = server.url("/");
        PushMechanism push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();

        pushNotification.deny(pushListenerFuture);
        pushListenerFuture.get();

        assertFalse(pushNotification.isApproved());
        assertFalse(pushNotification.isPending());
    }

    @Test
    public void testDenyFailureNetworkError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));

        StorageClient storageClient = mock(DefaultStorageClient.class);
        PushResponder.getInstance(storageClient);

        HttpUrl baseUrl = server.url("/");
        PushMechanism push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Void>();
        try{
            pushNotification.deny(pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof PushMechanismException);
            assertTrue(e.getLocalizedMessage().contains("Communication with server returned 404 code"));
            assertFalse(pushNotification.isApproved());
            assertTrue(pushNotification.isPending());
        }
    }

    @Test
    public void testDenyFailureAlreadyRejected() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_OK));

        StorageClient storageClient = mock(DefaultStorageClient.class);
        PushResponder.getInstance(storageClient);

        HttpUrl baseUrl = server.url("/");
        PushMechanism push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);
        pushNotification.setPending(false);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();
        try {
            pushNotification.deny(pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertFalse(pushNotification.isPending());
            assertTrue(e.getCause() instanceof PushMechanismException);
            assertTrue(e.getLocalizedMessage().contains("PushNotification is not in a valid status to authenticate;"));
        }
    }

    @Test
    public void testShouldParseToJsonSuccessfully() {
        String json = "{" +
                "\"id\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95-null\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"messageId\":\"AUTHENTICATE:63ca6f18-7cfb-4198-bcd0-ac5041fbbea01583798229441\"," +
                "\"challenge\":\"REMOVED\"," +
                "\"amlbCookie\":\"REMOVED\"," +
                "\"ttl\":120," +
                "\"approved\":false," +
                "\"pending\":true}";

        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTtl(TTL)
                .build();

        String pusNotificationAsJson = pushNotification.toJson();

        assertNotNull(pusNotificationAsJson);
        assertEquals(json, pusNotificationAsJson);
    }

    @Test
    public void testShouldSerializeSuccessfully() {
        String json = "{" +
                "\"id\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95-null\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"messageId\":\"AUTHENTICATE:63ca6f18-7cfb-4198-bcd0-ac5041fbbea01583798229441\"," +
                "\"challenge\":\"fZl8wu9JBxdRQ7miq3dE0fbF0Bcdd+gRETUbtl6qSuM=\"," +
                "\"amlbCookie\":\"ZnJfc3NvX2FtbGJfcHJvZD0wMQ==\"," +
                "\"ttl\":120," +
                "\"approved\":false," +
                "\"pending\":true}";

        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTtl(TTL)
                .build();

        String pusNotificationAsJson = pushNotification.serialize();

        assertNotNull(pusNotificationAsJson);
        assertEquals(json, pusNotificationAsJson);
    }

    @Test
    public void testShouldDeserializeSuccessfully() {
        String json = "{" +
                "\"id\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95-null\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"messageId\":\"AUTHENTICATE:63ca6f18-7cfb-4198-bcd0-ac5041fbbea01583798229441\"," +
                "\"challenge\":\"fZl8wu9JBxdRQ7miq3dE0fbF0Bcdd+gRETUbtl6qSuM=\"," +
                "\"amlbCookie\":\"ZnJfc3NvX2FtbGJfcHJvZD0wMQ==\"," +
                "\"ttl\":120," +
                "\"approved\":false," +
                "\"pending\":true}";

        PushNotification pushNotification = PushNotification.deserialize(json);

        assertNotNull(pushNotification);
        assertEquals(pushNotification.getMechanismUID(), MECHANISM_UID);
        assertEquals(pushNotification.getMessageId(), MESSAGE_ID);
        assertEquals(pushNotification.getChallenge(), CHALLENGE);
        assertEquals(pushNotification.getAmlbCookie(), AMLB_COOKIE);
        assertEquals(pushNotification.getTtl(), TTL);
    }

}
