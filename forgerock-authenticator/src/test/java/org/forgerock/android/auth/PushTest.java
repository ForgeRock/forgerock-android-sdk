/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PushTest extends FRABaseTest {

    @Test
    public void testCreatePushMechanismSuccessfuly() {
        Push mechanism = Push.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        assertEquals(mechanism.getMechanismUID(), MECHANISM_UID);
        assertEquals(mechanism.getIssuer(), ISSUER);
        assertEquals(mechanism.getAccountName(), ACCOUNT_NAME);
        assertEquals(mechanism.getType(), Mechanism.PUSH);
        assertEquals(mechanism.getRegistrationEndpoint(), REGISTRATION_ENDPOINT);
        assertEquals(mechanism.getAuthenticationEndpoint(), AUTHENTICATION_ENDPOINT);
        assertEquals(mechanism.getSecret(), SECRET);
    }

    @Test
    public void testShouldEqualEquivalentPushMechanism() {
        Mechanism mechanism1 = Push.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();
        Mechanism mechanism2 = Push.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        assertEquals(mechanism1, mechanism2);
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
        assertEquals(mechanism1.hashCode(), mechanism2.hashCode());
    }

    @Test
    public void testShouldNotEqualDifferentPushMechanismWithAccountName() {
        Mechanism mechanism1 = Push.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();
        Mechanism mechanism2 = Push.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(OTHER_ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

    @Test
    public void testShouldNotEqualDifferentPushMechanismWithAccountIssuer() {
        Mechanism mechanism1 = Push.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();
        Mechanism mechanism2 = Push.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(OTHER_ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(OTHER_AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(OTHER_REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

    @Test
    public void testShouldReturnAllNotifications() {
        Push mechanism = Push.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        PushNotification pushNotification1 = createPushNotification(MESSAGE_ID, mechanism);
        PushNotification pushNotification2 = createPushNotification(OTHER_MESSAGE_ID, mechanism);

        List<PushNotification> notificationList = new ArrayList<>();
        notificationList.add(pushNotification1);
        notificationList.add(pushNotification2);
        mechanism.setPushNotificationList(notificationList);

        assertEquals(2, mechanism.getAllNotifications().size());
    }

    @Test
    public void testShouldReturnOnlyPendingNotifications() {
        Push mechanism = Push.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        PushNotification pushNotification1 = createPushNotification(MESSAGE_ID, mechanism);
        PushNotification pushNotification2 = createPushNotification(OTHER_MESSAGE_ID, mechanism);
        PushNotification pushNotification3 = PushNotification.builder()
                .setMechanismUID(MECHANISM_UID)
                .setMessageId("s-o-m-e-i-d")
                .setChallenge(CHALLENGE)
                .setAmlbCookie(AMLB_COOKIE)
                .setPending(false)
                .setApproved(true)
                .setTtl(TTL)
                .build();

        List<PushNotification> notificationList = new ArrayList<>();
        notificationList.add(pushNotification1);
        notificationList.add(pushNotification2);
        notificationList.add(pushNotification3);
        mechanism.setPushNotificationList(notificationList);

        assertEquals(2, mechanism.getPendingNotifications().size());
    }

}