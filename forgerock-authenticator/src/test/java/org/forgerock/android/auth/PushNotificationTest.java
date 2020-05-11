/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import com.google.firebase.messaging.RemoteMessage;

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
    }

    @Test
    public void testCreateNotificationWithOptionalParametersSuccessfuly() {
        Calendar timeAdded = Calendar.getInstance();
        Calendar timeExpired = Calendar.getInstance();
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

        assertEquals(pushNotification1, pushNotification2);
        assertEquals(pushNotification1.compareTo(pushNotification2), 0);
        assertEquals(pushNotification2.compareTo(pushNotification1), 0);
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
        assertEquals(pushNotification1.compareTo(pushNotification2), 0);
        assertEquals(pushNotification2.compareTo(pushNotification1), 0);
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
        assertEquals(pushNotification1.compareTo(pushNotification2), 1);
        assertEquals(pushNotification2.compareTo(pushNotification1), -1);
    }

    @Test
    public void testShouldAcceptCorrectly() throws Exception {
        server.enqueue(new MockResponse());

        StorageClient storageClient = mock(DefaultStorageClient.class);

        HttpUrl baseUrl = server.url("/");
        Push push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Push.class))).willReturn(true);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();

        pushNotification.accept(pushListenerFuture);
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(HTTP_OK, responseCode);
        assertTrue(pushNotification.isApproved());
        assertFalse(pushNotification.isPending());
    }

    @Test
    public void testAcceptFailureNetworkError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));

        StorageClient storageClient = mock(DefaultStorageClient.class);

        HttpUrl baseUrl = server.url("/");
        Push push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Push.class))).willReturn(true);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();
        pushNotification.accept(pushListenerFuture);
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(HTTP_NOT_FOUND, responseCode);
        assertFalse(pushNotification.isApproved());
        assertTrue(pushNotification.isPending());
    }

    @Test
    public void testAcceptFailureAlreadyApproved() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_OK));

        StorageClient storageClient = mock(DefaultStorageClient.class);

        HttpUrl baseUrl = server.url("/");
        Push push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Push.class))).willReturn(true);
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
            Assert.fail("Should throw PushAuthenticationException");
        } catch (Exception e) {
            assertTrue(pushNotification.isApproved());
            assertFalse(pushNotification.isPending());
            assertTrue(e.getLocalizedMessage().contains("PushNotification is not in a valid status to authenticate;"));
        }
    }

    @Test
    public void testShouldDenyCorrectly() throws Exception {
        server.enqueue(new MockResponse());

        StorageClient storageClient = mock(DefaultStorageClient.class);

        HttpUrl baseUrl = server.url("/");
        Push push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Push.class))).willReturn(true);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();

        pushNotification.deny(pushListenerFuture);
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(HTTP_OK, responseCode);
        assertFalse(pushNotification.isApproved());
        assertFalse(pushNotification.isPending());
    }

    @Test
    public void testDenyFailureNetworkError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));

        StorageClient storageClient = mock(DefaultStorageClient.class);

        HttpUrl baseUrl = server.url("/");
        Push push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Push.class))).willReturn(true);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();
        pushNotification.deny(pushListenerFuture);
        int responseCode = (int) pushListenerFuture.get();

        assertEquals(HTTP_NOT_FOUND, responseCode);
        assertFalse(pushNotification.isApproved());
        assertTrue(pushNotification.isPending());
    }

    @Test
    public void testDenyFailureAlreadyRejected() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_OK));

        StorageClient storageClient = mock(DefaultStorageClient.class);

        HttpUrl baseUrl = server.url("/");
        Push push = mockPushMechanism(MECHANISM_UID, baseUrl.toString());

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(Push.class))).willReturn(true);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);
        pushNotification.setPending(false);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();
        try {
            pushNotification.deny(pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushAuthenticationException");
        } catch (Exception e) {
            assertFalse(pushNotification.isPending());
            assertTrue(e.getLocalizedMessage().contains("PushNotification is not in a valid status to authenticate;"));
        }
    }

}
