/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.util.Base64;

import com.google.firebase.messaging.RemoteMessage;

import org.forgerock.android.auth.exception.InvalidNotificationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class NotificationFactoryTest extends FRABaseTest {

    private StorageClient storageClient;
    private NotificationFactory notificationFactory;
    private PushMechanism push;

    @Before
    public void setUp() throws InvalidNotificationException {
        push = mockPushMechanism(MECHANISM_UID);

        storageClient = mock(DefaultStorageClient.class);
        given(storageClient.setAccount(any(Account.class))).willReturn(true);
        given(storageClient.setMechanism(any(PushMechanism.class))).willReturn(true);
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(true);
        given(storageClient.getMechanismByUUID(MECHANISM_UID)).willReturn(push);

        notificationFactory = new NotificationFactory(storageClient);
    }

    @Test
    public void testShouldHandleMessageAsParameters() throws Exception {
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        String messageId = remoteMessage.getData().get("messageId");
        String message = remoteMessage.getData().get("message");
        PushNotification pushNotification = notificationFactory.handleMessage(messageId, message);

        assertNotNull(pushNotification);
        assertEquals(MESSAGE_ID, pushNotification.getMessageId());
        assertEquals(CHALLENGE, pushNotification.getChallenge());
        assertEquals(new String(Base64.decode(AMLB_COOKIE, Base64.NO_WRAP)), pushNotification.getAmlbCookie());
        assertEquals(MECHANISM_UID, pushNotification.getMechanismUID());
        assertEquals(TTL, pushNotification.getTtl());
    }

    @Test
    public void testShouldHandleRemoteMessage() throws Exception {
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());
        PushNotification pushNotification = notificationFactory.handleMessage(remoteMessage);

        assertNotNull(pushNotification);
        assertEquals(MESSAGE_ID, pushNotification.getMessageId());
        assertEquals(CHALLENGE, pushNotification.getChallenge());
        assertEquals(new String(Base64.decode(AMLB_COOKIE, Base64.NO_WRAP)), pushNotification.getAmlbCookie());
        assertEquals(MECHANISM_UID, pushNotification.getMechanismUID());
        assertEquals(TTL, pushNotification.getTtl());
    }

    @Test
    public void testShouldNotHandleRemoteMessageWrongSecret() throws Exception {
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, INCORRECT_SECRET, generateBaseMessage());

        try {
            notificationFactory.handleMessage(remoteMessage);
            Assert.fail("Should throw InvalidNotificationException");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidNotificationException);
            assertTrue(e.getLocalizedMessage().equals("Failed to validate jwt within the remote message."));
        }
    }

    @Test
    public void testShouldNotHandleRemoteMessageNotJwtMessage() throws Exception {
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, null, generateBaseMessage());

        try {
            notificationFactory.handleMessage(remoteMessage);
            Assert.fail("Should throw InvalidNotificationException");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidNotificationException);
            assertTrue(e.getLocalizedMessage().equals("Failed to reconstruct JWT for the remote message."));
        }
    }

    @Test
    public void testShouldNotHandleRemoteInvalidTTL() throws Exception {
        Map<String, String> baseMessage = generateBaseMessage();
        baseMessage.put(PushParser.TTL, "INVALID");
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, baseMessage);

        try {
            notificationFactory.handleMessage(remoteMessage);
            Assert.fail("Should throw InvalidNotificationException");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidNotificationException);
            assertTrue(e.getLocalizedMessage().equals("Failed to reconstruct JWT for the remote message. TTL was not a number."));
        }
    }

    @Test
    public void testShouldNotHandleRemoteMissingMechanismUUID() throws Exception {
        Map<String, String> baseMessage = generateBaseMessage();
        baseMessage.remove(PushParser.MECHANISM_UID);
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, baseMessage);

        try {
            notificationFactory.handleMessage(remoteMessage);
            Assert.fail("Should throw InvalidNotificationException");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidNotificationException);
            assertTrue(e.getLocalizedMessage().equals("Remote message did not contain required fields."));
        }
    }

    @Test
    public void testShouldNotHandleRemoteMissingChallenge() throws Exception {
        Map<String, String> baseMessage = generateBaseMessage();
        baseMessage.remove(PushParser.CHALLENGE);
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, baseMessage);

        try {
            notificationFactory.handleMessage(remoteMessage);
            Assert.fail("Should throw InvalidNotificationException");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidNotificationException);
            assertTrue(e.getLocalizedMessage().equals("Remote message did not contain required fields."));
        }
    }

    @Test
    public void testShouldNotHandleRemoteMissingMessageId() throws Exception {
        Map<String, String> baseMessage = generateBaseMessage();
        baseMessage.remove(PushParser.MESSAGE_ID);
        RemoteMessage remoteMessage = generateMockRemoteMessage(null, CORRECT_SECRET, baseMessage);

        try {
            notificationFactory.handleMessage(remoteMessage);
            Assert.fail("Should throw InvalidNotificationException");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidNotificationException);
            assertTrue(e.getLocalizedMessage().equals("Remote message did not contain required fields."));
        }
    }

    @Test
    public void testShouldNotHandleRemoteInvalidMechanismUUID() throws Exception {
        Map<String, String> baseMessage = generateBaseMessage();
        baseMessage.put(PushParser.MECHANISM_UID, "INVALID");
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, baseMessage);

        try {
            notificationFactory.handleMessage(remoteMessage);
            Assert.fail("Should throw InvalidNotificationException");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidNotificationException);
            assertTrue(e.getLocalizedMessage().equals("Could not retrieve the PUSH mechanism associated with this remote message."));
        }
    }

    @Test
    public void testShouldNotHandleRemoteUnableToPersist() throws Exception {
        given(storageClient.setNotification(any(PushNotification.class))).willReturn(false);
        RemoteMessage remoteMessage = generateMockRemoteMessage(MESSAGE_ID, CORRECT_SECRET, generateBaseMessage());

        try {
            notificationFactory.handleMessage(remoteMessage);
            Assert.fail("Should throw InvalidNotificationException");
        } catch (Exception e) {
            assertTrue(e instanceof InvalidNotificationException);
            assertTrue(e.getLocalizedMessage().equals("Unable to store Push Notification on the target stored system."));
        }
    }

}
