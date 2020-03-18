/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class OathTest extends BaseTest {

    @Test
    public void testCreateOathMechanism() {
        Oath mechanism = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.OATH,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        assertEquals(mechanism.getMechanismUID(), MECHANISM_UID);
        assertEquals(mechanism.getIssuer(), ISSUER);
        assertEquals(mechanism.getAccountName(), ACCOUNT_NAME);
        assertEquals(mechanism.getType(), Mechanism.OATH);
        assertEquals(mechanism.getOathType(), Oath.TokenType.HOTP);
        assertEquals(mechanism.getAlgorithm(), ALGORITHM);
        assertEquals(mechanism.getSecret(), SECRET);
        assertEquals(mechanism.getDigits(), DIGITS);
        assertEquals(mechanism.getCounter(), COUNTER);
        assertEquals(mechanism.getPeriod(), PERIOD);
    }

    @Test
    public void testShouldBeEqualEquivalentOathMechanism() {
        Mechanism mechanism1 = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.OATH,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        Mechanism mechanism2
                = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.OATH,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        assertEquals(mechanism1, mechanism2);
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
        assertEquals(mechanism1.hashCode(), mechanism2.hashCode());
    }

    @Test
    public void testShouldNotBeEqualDifferentOathMechanismWithType() {
        Mechanism mechanism1 = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.OATH,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        Mechanism mechanism2
                = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.PUSH,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), -1);
        assertEquals(mechanism2.compareTo(mechanism1), 1);
    }

    @Test
    public void testShouldNotBeEqualDifferentOathMechanismWithAccountName() {
        Mechanism mechanism1 = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.OATH,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        Mechanism mechanism2
                = new Oath(MECHANISM_UID, ISSUER, OTHER_ACCOUNT_NAME, Mechanism.OATH,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

    @Test
    public void testShouldNotBeEqualDifferentOathMechanismWithAccountIssuer() {
        Mechanism mechanism1 = new Oath(MECHANISM_UID, ISSUER, ACCOUNT_NAME, Mechanism.OATH,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);
        Mechanism mechanism2
                = new Oath(MECHANISM_UID, OTHER_ISSUER, ACCOUNT_NAME, Mechanism.OATH,
                Oath.TokenType.HOTP, ALGORITHM, SECRET, DIGITS, COUNTER, PERIOD);

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

}