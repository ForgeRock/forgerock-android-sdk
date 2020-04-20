/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class NotificationTest extends BaseTest {

    @Test
    public void createNotificationSuccessfuly() {
        Calendar timeAdded = Calendar.getInstance();
        Calendar timeExpired = Calendar.getInstance();

        Notification notification = Notification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTimeExpired(timeExpired)
                .setTtl(TTL)
                .build();

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
        Calendar timeExpired = Calendar.getInstance();
        boolean approved = false;
        boolean pending = true;

        Notification notification = Notification.builder()
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
    public void shouldBeEqualEquivalentNotification() {
        Calendar timeAdded = Calendar.getInstance();
        Notification notification1 = Notification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .build();
        Notification notification2 = Notification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .build();

        assertEquals(notification1, notification2);
        assertEquals(notification1.compareTo(notification2), 0);
        assertEquals(notification2.compareTo(notification1), 0);
        assertEquals(notification1.hashCode(), notification2.hashCode());
    }

    @Test
    public void shouldNotBeEqualNotificationWithDifferentMechanismUID() {
        Calendar timeAdded = Calendar.getInstance();
        Notification notification1 = Notification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .build();
        Notification notification2 = Notification.builder()
                .setMechanismUID(OTHER_MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded)
                .setTtl(TTL)
                .build();

        assertFalse(notification1.equals(notification2));
        assertEquals(notification1.compareTo(notification2), 0);
        assertEquals(notification2.compareTo(notification1), 0);
    }

    @Test
    public void shouldNotBeEqualNotificationWithDifferentTimeAdded() {
        Calendar timeAdded1 = Calendar.getInstance();

        long time = timeAdded1.getTimeInMillis();
        Calendar timeAdded2 = Calendar.getInstance();
        timeAdded2.setTimeInMillis(time+100);

        Notification notification1 = Notification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded1)
                .setTtl(TTL)
                .build();
        Notification notification2 = Notification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId(MESSAGE_ID)
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setTimeAdded(timeAdded2)
                .setTtl(TTL)
                .build();

        assertFalse(notification1.equals(notification2));
        assertEquals(notification1.compareTo(notification2), 1);
        assertEquals(notification2.compareTo(notification1), -1);
    }

}