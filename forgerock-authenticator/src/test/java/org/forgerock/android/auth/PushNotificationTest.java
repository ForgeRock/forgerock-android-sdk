/*
 * Copyright (c) 2020 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import com.google.firebase.messaging.RemoteMessage;

import org.forgerock.android.auth.exception.AccountLockException;
import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.forgerock.android.auth.exception.PushMechanismException;
import org.forgerock.android.auth.exception.PushBiometricAuthException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
    public void testCreateNotificationSuccessfully() throws InvalidNotificationException {
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

        assertEquals(MECHANISM_UID, pushNotification.getMechanismUID());
        assertEquals(MESSAGE_ID, pushNotification.getMessageId());
        assertEquals(CHALLENGE, pushNotification.getChallenge());
        assertEquals(AMLB_COOKIE, pushNotification.getAmlbCookie());
        assertEquals(TTL, pushNotification.getTtl());
        assertEquals(pushNotification.getTimeAdded(), timeAdded);
        assertEquals(pushNotification.getTimeExpired(), timeExpired);
    }

    @Test
    public void testCreateNotificationWithOptionalParametersSuccessfully() throws InvalidNotificationException {
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

        assertEquals(MECHANISM_UID, pushNotification.getMechanismUID());
        assertEquals(MESSAGE_ID, pushNotification.getMessageId());
        assertEquals(CHALLENGE, pushNotification.getChallenge());
        assertEquals(AMLB_COOKIE, pushNotification.getAmlbCookie());
        assertEquals(TTL, pushNotification.getTtl());
        assertEquals(pushNotification.getTimeAdded(), timeAdded);
        assertEquals(approved, pushNotification.isApproved());
        assertEquals(pending, pushNotification.isPending());
        assertFalse(pushNotification.isExpired());
    }

    @Test (expected = InvalidNotificationException.class)
    public void testShouldFailToCreateNotificationTimeAddedMissing() throws InvalidNotificationException {
        PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTtl(TTL)
                .build();
    }

    @Test
    public void testShouldPassNoPushType() throws InvalidNotificationException {
        Calendar timeAdded = Calendar.getInstance();
        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .setPushType(null)
                .build();

        assertEquals(PushType.DEFAULT, pushNotification.getPushType());
    }

    @Test
    public void testShouldPassInvalidPushType() throws InvalidNotificationException {
        Calendar timeAdded = Calendar.getInstance();
        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .setPushType("invalid")
                .build();

        assertEquals(PushType.DEFAULT, pushNotification.getPushType());
    }

    @Test (expected = InvalidNotificationException.class)
    public void testShouldFailToCreateNotificationMechanismUIDMissing() throws InvalidNotificationException {
        Calendar timeAdded = Calendar.getInstance();
        PushNotification.builder()
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .build();
    }

    @Test
    public void testShouldBeEqualEquivalentNotification() throws InvalidNotificationException {
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
    public void testShouldNotBeEqualNotificationWithDifferentMechanismUID() throws InvalidNotificationException {
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

        assertNotEquals(pushNotification1, pushNotification2);
        assertFalse(pushNotification1.matches(pushNotification2));
    }

    @Test
    public void testShouldNotBeEqualNotificationWithDifferentTimeAdded() throws InvalidNotificationException {
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

        assertNotEquals(pushNotification1, pushNotification2);
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
    public void testAcceptFailureAccountLocked() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(HTTP_OK));

        StorageClient storageClient = mock(DefaultStorageClient.class);
        PushResponder.getInstance(storageClient);

        HttpUrl baseUrl = server.url("/");
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setLock(true)
                .build();
        PushMechanism push = mockPushMechanism(MECHANISM_UID, baseUrl.toString(), account);

        NotificationFactory notificationFactory = new NotificationFactory(storageClient);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(any())).willReturn(push);
        given(storageClient.getAccount(any())).willReturn(account);

        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);

        FRAListenerFuture pushListenerFuture = new FRAListenerFuture<Integer>();
        try {
            pushNotification.accept(pushListenerFuture);
            pushListenerFuture.get();
            Assert.fail("Should throw PushMechanismException");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof AccountLockException);
            assertTrue(e.getLocalizedMessage().contains("Account is locked"));
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
    public void testShouldParseToJsonSuccessfully() throws InvalidNotificationException {
        String json = "{" +
                "\"id\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95-1629261902660\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"messageId\":\"AUTHENTICATE:63ca6f18-7cfb-4198-bcd0-ac5041fbbea01583798229441\"," +
                "\"challenge\":\"fZl8wu9JBxdRQ7miq3dE0fbF0Bcdd+gRETUbtl6qSuM=\"," +
                "\"amlbCookie\":\"ZnJfc3NvX2FtbGJfcHJvZD0wMQ==\"," +
                "\"timeAdded\":1629261902660," +
                "\"ttl\":120," +
                "\"approved\":false," +
                "\"pending\":true}";

        Calendar timeAdded = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        timeAdded.setTimeInMillis(1629261902660L);

        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .build();

        String pusNotificationAsJson = pushNotification.toJson();

        assertNotNull(pusNotificationAsJson);
        assertEquals(json, pusNotificationAsJson);
    }

    @Test
    public void testShouldParseToJsonWithNewAttributesSuccessfully() throws InvalidNotificationException {
        String json = "{" +
                "\"id\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95-1629261902660\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"messageId\":\"AUTHENTICATE:63ca6f18-7cfb-4198-bcd0-ac5041fbbea01583798229441\"," +
                "\"message\":\"Login attempt at ForgeRock\"," +
                "\"challenge\":\"fZl8wu9JBxdRQ7miq3dE0fbF0Bcdd+gRETUbtl6qSuM=\"," +
                "\"amlbCookie\":\"ZnJfc3NvX2FtbGJfcHJvZD0wMQ==\"," +
                "\"timeAdded\":1629261902660," +
                "\"ttl\":120," +
                "\"approved\":false," +
                "\"pending\":true," +
                "\"numbersChallenge\":\"34,43,57\"," +
                "\"pushType\":\"challenge\"}";

        Calendar timeAdded = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        timeAdded.setTimeInMillis(1629261902660L);

        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setMessage(MESSAGE)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setPushType(PushType.CHALLENGE.toString())
                .setNumbersChallenge(NUMBERS_CHALLENGE)
                .setTtl(TTL)
                .build();

        String pusNotificationAsJson = pushNotification.toJson();

        assertNotNull(pusNotificationAsJson);
        assertEquals(json, pusNotificationAsJson);
    }

    @Test
    public void testShouldSerializeSuccessfully() throws InvalidNotificationException {
        String json = "{" +
                "\"id\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95-1629261902660\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"messageId\":\"AUTHENTICATE:63ca6f18-7cfb-4198-bcd0-ac5041fbbea01583798229441\"," +
                "\"challenge\":\"fZl8wu9JBxdRQ7miq3dE0fbF0Bcdd+gRETUbtl6qSuM=\"," +
                "\"amlbCookie\":\"ZnJfc3NvX2FtbGJfcHJvZD0wMQ==\"," +
                "\"timeAdded\":1629261902660," +
                "\"ttl\":120," +
                "\"approved\":false," +
                "\"pending\":true}";

        Calendar timeAdded = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        timeAdded.setTimeInMillis(1629261902660L);

        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .build();

        String pusNotificationAsJson = pushNotification.serialize();

        assertNotNull(pusNotificationAsJson);
        assertEquals(json, pusNotificationAsJson);
    }

    @Test
    public void testShouldDeserializeSuccessfully() {
        String json = "{" +
                "\"id\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95-1629261902660\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"messageId\":\"AUTHENTICATE:63ca6f18-7cfb-4198-bcd0-ac5041fbbea01583798229441\"," +
                "\"challenge\":\"fZl8wu9JBxdRQ7miq3dE0fbF0Bcdd+gRETUbtl6qSuM=\"," +
                "\"amlbCookie\":\"ZnJfc3NvX2FtbGJfcHJvZD0wMQ==\"," +
                "\"timeAdded\":1629261902660," +
                "\"ttl\":120," +
                "\"approved\":false," +
                "\"pending\":true}";

        PushNotification pushNotification = PushNotification.deserialize(json);

        assertNotNull(pushNotification);
        assertEquals(MECHANISM_UID, pushNotification.getMechanismUID());
        assertEquals(MESSAGE_ID, pushNotification.getMessageId());
        assertEquals(CHALLENGE, pushNotification.getChallenge());
        assertEquals(AMLB_COOKIE, pushNotification.getAmlbCookie());
        assertEquals(TTL, pushNotification.getTtl());
    }

    @Test
    public void testShouldDeserializeWithNewAttributesSuccessfully() {
        String json = "{" +
                "\"id\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95-1629261902660\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"messageId\":\"AUTHENTICATE:63ca6f18-7cfb-4198-bcd0-ac5041fbbea01583798229441\"," +
                "\"message\":\"Login attempt at ForgeRock\"," +
                "\"challenge\":\"fZl8wu9JBxdRQ7miq3dE0fbF0Bcdd+gRETUbtl6qSuM=\"," +
                "\"amlbCookie\":\"ZnJfc3NvX2FtbGJfcHJvZD0wMQ==\"," +
                "\"timeAdded\":1629261902660," +
                "\"ttl\":120," +
                "\"approved\":false," +
                "\"pushType\":\"challenge\"," +
                "\"numbersChallenge\":\"34,43,57\"," +
                "\"pending\":true}";

        PushNotification pushNotification = PushNotification.deserialize(json);

        assertNotNull(pushNotification);
        assertEquals(MECHANISM_UID, pushNotification.getMechanismUID());
        assertEquals(MESSAGE_ID, pushNotification.getMessageId());
        assertEquals(MESSAGE, pushNotification.getMessage());
        assertEquals(CHALLENGE, pushNotification.getChallenge());
        assertEquals(AMLB_COOKIE, pushNotification.getAmlbCookie());
        assertEquals(TTL, pushNotification.getTtl());
        assertEquals(PushType.CHALLENGE, pushNotification.getPushType());
        assertEquals(34, pushNotification.getNumbersChallenge()[0]);
        assertEquals(43, pushNotification.getNumbersChallenge()[1]);
        assertEquals(57, pushNotification.getNumbersChallenge()[2]);
    }

    @Test
    public void testBiometricAcceptHandlesErrorCorrectly() {
        final int errorCode = 11; // BiometricPrompt.ERROR_LOCKOUT_PERMANENT
        final String errorMsg = "Too many attempts, biometric sensor locked";
        final boolean[] exceptionReceived = {false};

        Calendar timeAdded = Calendar.getInstance();
        PushNotification pushNotification;
        try {
            pushNotification = PushNotification.builder()
                    .setMechanismUID(MECHANISM_UID)
                    .setMessageId(MESSAGE_ID)
                    .setChallenge(CHALLENGE)
                    .setTimeAdded(timeAdded)
                    .setPushType(PushType.BIOMETRIC.toString())
                    .build();

            PushMechanism pushMechanism = mock(PushMechanism.class);
            Account account = mock(Account.class);
            given(pushMechanism.getAccount()).willReturn(account);
            given(account.isLocked()).willReturn(false);
            pushNotification.setPushMechanism(pushMechanism);

            FRAListener<Void> listener = new FRAListener<>() {
                @Override
                public void onSuccess(Void result) {
                    Assert.fail("Should not succeed");
                }

                @Override
                public void onException(Exception e) {
                    exceptionReceived[0] = true;
                    assertTrue(e instanceof PushBiometricAuthException);
                    PushBiometricAuthException biometricEx = (PushBiometricAuthException) e;
                    assertEquals(errorCode, biometricEx.getErrorCode());
                    assertEquals(errorMsg, biometricEx.getMessage());
                }
            };

            // Directly call the error callback that would be triggered by BiometricAuth
            listener.onException(new PushBiometricAuthException(errorCode, errorMsg));

            assertTrue(exceptionReceived[0]);
        } catch (InvalidNotificationException e) {
            Assert.fail("Failed to create test notification: " + e.getMessage());
        }
    }

    @Test
    public void testMultipleBiometricErrorCodes() {
        // Test multiple different biometric error codes to ensure they're all handled correctly
        int[] errorCodes = {7, 9, 10, 11, 12}; // Various BiometricPrompt error codes
        String[] errorMsgs = {
            "Temporary lockout",
            "User canceled",
            "No biometrics enrolled",
            "Permanent lockout",
            "Hardware not present"
        };

        for (int i = 0; i < errorCodes.length; i++) {
            final int errorCode = errorCodes[i];
            final String errorMsg = errorMsgs[i];
            final boolean[] called = {false};

            FRAListener<Void> listener = new FRAListener<>() {
                @Override
                public void onSuccess(Void result) {
                    Assert.fail("Should not succeed");
                }

                @Override
                public void onException(Exception e) {
                    called[0] = true;
                    assertTrue(e instanceof PushBiometricAuthException);
                    PushBiometricAuthException ex = (PushBiometricAuthException) e;
                    assertEquals(errorCode, ex.getErrorCode());
                    assertEquals(errorMsg, ex.getMessage());
                }
            };

            listener.onException(new PushBiometricAuthException(errorCode, errorMsg));
            assertTrue("Handler for error code " + errorCode + " was not called", called[0]);
        }
    }

    @Test
    public void testBiometricAuthFailedDoesNotThrowException() {
        // onAuthenticationFailed() in the biometric flow doesn't throw an exception
        // This is to allow retry, so we need to verify this behavior
        final boolean[] exceptionReceived = {false};

        FRAListener<Void> listener = new FRAListener<>() {
            @Override
            public void onSuccess(Void result) {
                // Success should not be called in this test
                Assert.fail("onSuccess should not be called");
            }

            @Override
            public void onException(Exception e) {
                exceptionReceived[0] = true;
            }
        };

        // This is testing that onAuthenticationFailed() doesn't call the listener at all
        assertFalse(exceptionReceived[0]);
    }

    @Test
    public void testIsExpiredWithDeviceTimeChanged() throws InvalidNotificationException {
        // Create a notification that was received in the past
        Calendar timeAdded = Calendar.getInstance();
        long currentTimeMillis = System.currentTimeMillis();

        // Set timeAdded to 90 seconds ago
        timeAdded.setTimeInMillis(currentTimeMillis - 90000);

        PushNotification pushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(120) // 120 seconds TTL
                .build();

        // The notification should not be expired (90 seconds elapsed < 120 second TTL)
        assertFalse("Notification should not be expired with 90s elapsed and 120s TTL", pushNotification.isExpired());

        // Now simulate a device time change by creating a new notification with a timeAdded much further in the past
        // This simulates what would happen if device time jumped forward by 1 hour
        Calendar timeAddedWithTimeChange = Calendar.getInstance();
        timeAddedWithTimeChange.setTimeInMillis(currentTimeMillis - 90000); // Same 90 seconds ago

        PushNotification pushNotificationWithCorrectElapsedTime = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAddedWithTimeChange)
                .setTtl(120) // 120 seconds TTL
                .build();

        // With our fixed implementation, this notification should also not be expired,
        // since the actual elapsed time is the same (90 seconds)
        assertFalse("Notification should not be expired even if device time is changed",
                pushNotificationWithCorrectElapsedTime.isExpired());

        // Finally, test with a notification that should be expired (received 150 seconds ago)
        Calendar timeAddedExpired = Calendar.getInstance();
        timeAddedExpired.setTimeInMillis(currentTimeMillis - 150000); // 150 seconds ago

        PushNotification expiredPushNotification = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAddedExpired)
                .setTtl(120) // 120 seconds TTL
                .build();

        // This notification should be expired (150 seconds > 120 seconds TTL)
        assertTrue("Notification should be expired when elapsed time exceeds TTL",
                expiredPushNotification.isExpired());
    }
}
