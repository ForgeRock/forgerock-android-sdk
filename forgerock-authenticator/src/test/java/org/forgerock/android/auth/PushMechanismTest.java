/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PushMechanismTest extends FRABaseTest {

    @Test
    public void testCreatePushMechanismSuccessfuly() {
        PushMechanism mechanism = PushMechanism.builder()
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
        Mechanism mechanism1 = PushMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();
        Mechanism mechanism2 = PushMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        assertEquals(mechanism1, mechanism2);
        assertTrue(mechanism1.matches(mechanism2));
        assertEquals(mechanism1.hashCode(), mechanism2.hashCode());
    }

    @Test
    public void testShouldNotEqualDifferentPushMechanismWithAccountName() {
        Mechanism mechanism1 = PushMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();
        Mechanism mechanism2 = PushMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(OTHER_ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        assertFalse(mechanism1.equals(mechanism2));
        assertFalse(mechanism1.matches(mechanism2));
    }

    @Test
    public void testShouldNotEqualDifferentPushMechanismWithAccountIssuer() {
        Mechanism mechanism1 = PushMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();
        Mechanism mechanism2 = PushMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(OTHER_ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(OTHER_AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(OTHER_REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        assertFalse(mechanism1.equals(mechanism2));
        assertFalse(mechanism1.matches(mechanism2));
    }

    @Test
    public void testShouldReturnAllNotifications() {
        PushMechanism mechanism = PushMechanism.builder()
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
        PushMechanism mechanism = PushMechanism.builder()
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

    @Test
    public void testShouldParseToJsonSuccessfully() {
        String json = "{" +
                "\"id\":\"issuer1-user1-pushauth\"," +
                "\"issuer\":\"issuer1\"," +
                "\"accountName\":\"user1\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"secret\":\"REMOVED\"," +
                "\"type\":\"pushauth\"," +
                "\"registrationEndpoint\":\"REMOVED\"," +
                "\"authenticationEndpoint\":\"REMOVED\"" +
                "}";

        PushMechanism mechanism = PushMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        String mechanismAsJson = mechanism.toJson();

        assertNotNull(mechanismAsJson);
        assertEquals(json, mechanismAsJson);
    }

    @Test
    public void testShouldSerializeSuccessfully() {
        String json = "{" +
                "\"id\":\"issuer1-user1-pushauth\"," +
                "\"issuer\":\"issuer1\"," +
                "\"accountName\":\"user1\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"secret\":\"JMEZ2W7D462P3JYBDG2HV7PFBM\"," +
                "\"type\":\"pushauth\"," +
                "\"registrationEndpoint\":\"http:\\/\\/openam.forgerock.com:8080\\/openam\\/json\\/push\\/sns\\/message?_action=register\"," +
                "\"authenticationEndpoint\":\"http:\\/\\/openam.forgerock.com:8080\\/openam\\/json\\/push\\/sns\\/message?_action=authenticate\"" +
                "}";

        PushMechanism mechanism = PushMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        String mechanismAsJson = mechanism.serialize();

        assertNotNull(mechanismAsJson);
        assertEquals(json, mechanismAsJson);
    }

    @Test
    public void testShouldDeserializeSuccessfully() {
        String json = "{" +
                "\"id\":\"issuer1-user1-pushauth\"," +
                "\"issuer\":\"issuer1\"," +
                "\"accountName\":\"user1\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"secret\":\"JMEZ2W7D462P3JYBDG2HV7PFBM\"," +
                "\"type\":\"pushauth\"," +
                "\"registrationEndpoint\":\"http:\\/\\/openam.forgerock.com:8080\\/openam\\/json\\/push\\/sns\\/message?_action=register\"," +
                "\"authenticationEndpoint\":\"http:\\/\\/openam.forgerock.com:8080\\/openam\\/json\\/push\\/sns\\/message?_action=authenticate\"" +
                "}";

        PushMechanism mechanism = PushMechanism.deserialize(json);

        assertNotNull(mechanism);
        assertEquals(mechanism.getMechanismUID(), MECHANISM_UID);
        assertEquals(mechanism.getIssuer(), ISSUER);
        assertEquals(mechanism.getAccountName(), ACCOUNT_NAME);
        assertEquals(mechanism.getType(), Mechanism.PUSH);
        assertEquals(mechanism.getRegistrationEndpoint(), REGISTRATION_ENDPOINT);
        assertEquals(mechanism.getAuthenticationEndpoint(), AUTHENTICATION_ENDPOINT);
        assertEquals(mechanism.getSecret(), SECRET);
    }

}