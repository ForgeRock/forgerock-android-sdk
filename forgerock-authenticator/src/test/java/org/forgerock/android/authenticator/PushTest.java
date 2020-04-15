package org.forgerock.android.authenticator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

public class PushTest extends BaseTest {

    @Test
    public void createPushMechanismSuccessfuly() {
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
    public void shouldEqualEquivalentPushMechanism() {
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
    public void shouldNotEqualDifferentPushMechanismWithAccountName() {
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
    public void shouldNotEqualDifferentPushMechanismWithAccountIssuer() {
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
                .setAuthenticationEndpoint(AUTHENTICATION_ENDPOINT)
                .setRegistrationEndpoint(REGISTRATION_ENDPOINT)
                .setSecret(SECRET)
                .build();

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

}