package org.forgerock.android.authenticator;

import android.net.Uri;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class NotificationTest {

    private final String MECHANISM_UID = "b162b325-ebb1-48e0-8ab7-b38cf341da95";
    private final String OTHER_MECHANISM_UID = "013be51a-8c14-356d-b0fc-b3660cc8a101";
    private final String MESSAGE_ID = "AUTHENTICATE:63ca6f18-7cfb-4198-bcd0-ac5041fbbea01583798229441";
    private final String CHALLENGE = "fZl8wu9JBxdRQ7miq3dE0fbF0Bcdd+gRETUbtl6qSuM=";
    private final String AMLB_COOKIE = "ZnJfc3NvX2FtbGJfcHJvZD0wMQ==";
    private final long TTL = 120;

    @Test
    public void createNotificationSuccessfuly() {
        Calendar timeAdded = Calendar.getInstance();
        Notification notification = new Notification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);

        assertEquals(notification.getMechanismUID(), MECHANISM_UID);
        assertEquals(notification.getMessageId(), MESSAGE_ID);
        assertEquals(notification.getChallenge(), CHALLENGE);
        assertEquals(notification.getAmlbCookie(), AMLB_COOKIE);
        assertEquals(notification.getTtl(), TTL);
        assertEquals(notification.getTimeAdded(), timeAdded);
    }

    @Test
    public void createNotificationWithOptionalParametersSuccessfuly() {
        Calendar timeAdded = Calendar.getInstance();
        boolean approved = false;
        boolean pending = true;

        Notification notification = new Notification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL, approved, pending);

        assertEquals(notification.getMechanismUID(), MECHANISM_UID);
        assertEquals(notification.getMessageId(), MESSAGE_ID);
        assertEquals(notification.getChallenge(), CHALLENGE);
        assertEquals(notification.getAmlbCookie(), AMLB_COOKIE);
        assertEquals(notification.getTtl(), TTL);
        assertEquals(notification.getTimeAdded(), timeAdded);
        assertEquals(notification.isApproved(), approved);
        assertEquals(notification.isPending(), pending);
    }

    @Test
    public void shouldEqualEquivalentNotification() {
        Calendar timeAdded = Calendar.getInstance();
        Notification notification1 = new Notification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);
        Notification notification2 = new Notification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);

        assertEquals(notification1, notification2);
        assertEquals(notification1.compareTo(notification2), 0);
        assertEquals(notification2.compareTo(notification1), 0);
        assertEquals(notification1.hashCode(), notification2.hashCode());
    }

    @Test
    public void shouldNotEqualDifferentNotificationWithMechanismUID() {
        Calendar timeAdded = Calendar.getInstance();
        Notification notification1 = new Notification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);
        Notification notification2 = new Notification(OTHER_MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded, TTL);

        assertFalse(notification1.equals(notification2));
        assertEquals(notification1.compareTo(notification2), 0);
        assertEquals(notification2.compareTo(notification1), 0);
    }

    @Test
    public void shouldNotEqualDifferentNotificationTimeAdded() {
        Calendar timeAdded1 = Calendar.getInstance();

        long time = timeAdded1.getTimeInMillis();
        Calendar timeAdded2 = Calendar.getInstance();
        timeAdded2.setTimeInMillis(time+100);

        Notification notification1 = new Notification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded1, TTL);
        Notification notification2 = new Notification(MECHANISM_UID, MESSAGE_ID, CHALLENGE,
                AMLB_COOKIE, timeAdded2, TTL);

        assertFalse(notification1.equals(notification2));
        assertEquals(notification1.compareTo(notification2), 1);
        assertEquals(notification2.compareTo(notification1), -1);
    }

}