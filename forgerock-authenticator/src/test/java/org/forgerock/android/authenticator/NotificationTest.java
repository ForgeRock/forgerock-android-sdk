package org.forgerock.android.authenticator;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

public class NotificationTest extends BaseTest {

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