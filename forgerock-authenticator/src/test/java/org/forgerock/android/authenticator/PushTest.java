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
        Push mechanism = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.PUSH,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        assertEquals(mechanism.getMechanismUID(), MECHANISM_UID);
        assertEquals(mechanism.getIssuer(), ISSUER);
        assertEquals(mechanism.getAccountName(), ACCOUNT_NAME);
        assertEquals(mechanism.getType(), Mechanism.PUSH);
        assertEquals(mechanism.getRegistrationEndpoint(), REGISTRATION_ENDPOINT);
        assertEquals(mechanism.getAuthenticationEndpoint(), AUTHENTICATION_ENDPOINT);
    }

    @Test
    public void shouldEqualEquivalentPushMechanism() {
        Mechanism mechanism1 = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.PUSH,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        Mechanism mechanism2
                = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.PUSH,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        assertEquals(mechanism1, mechanism2);
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
        assertEquals(mechanism1.hashCode(), mechanism2.hashCode());
    }

    @Test
    public void shouldNotEqualDifferentPushMechanismWithType() {
        Mechanism mechanism1 = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.PUSH,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        Mechanism mechanism2
                = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.OATH,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 1);
        assertEquals(mechanism2.compareTo(mechanism1), -1);
    }

    @Test
    public void shouldNotEqualDifferentPushMechanismWithAccountName() {
        Mechanism mechanism1 = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.PUSH,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        Mechanism mechanism2
                = new Push(MECHANISM_UID, ISSUER, OTHER_ACCOUNT_NAME, Mechanism.PUSH,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

    @Test
    public void shouldNotEqualDifferentPushMechanismWithAccountIssuer() {
        Mechanism mechanism1 = new Push(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.PUSH,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);
        Mechanism mechanism2
                = new Push(MECHANISM_UID, OTHER_ISSUER, ACCOUNT_NAME, Mechanism.PUSH,
                REGISTRATION_ENDPOINT, AUTHENTICATION_ENDPOINT);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

}