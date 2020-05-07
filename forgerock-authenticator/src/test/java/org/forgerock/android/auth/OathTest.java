/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class OathTest extends FRABaseTest {

    @Test
    public void testCreateOathMechanism() {
        Oath mechanism = Oath.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setType(Oath.TokenType.HOTP)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .setPeriod(PERIOD)
                .build();

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
        Mechanism mechanism1 = Oath.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setType(Oath.TokenType.HOTP)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .setPeriod(PERIOD)
                .build();
        Mechanism mechanism2 = Oath.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setType(Oath.TokenType.HOTP)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .setPeriod(PERIOD)
                .build();

        assertEquals(mechanism1, mechanism2);
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
        assertEquals(mechanism1.hashCode(), mechanism2.hashCode());
    }

    @Test
    public void testShouldNotBeEqualDifferentOathMechanismWithAccountName() {
        Mechanism mechanism1 = Oath.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setType(Oath.TokenType.HOTP)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .setPeriod(PERIOD)
                .build();
        Mechanism mechanism2 = Oath.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(OTHER_ACCOUNT_NAME)
                .setType(Oath.TokenType.HOTP)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .setPeriod(PERIOD)
                .build();

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

    @Test
    public void testShouldNotBeEqualDifferentOathMechanismWithAccountIssuer() {
        Mechanism mechanism1 = Oath.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setType(Oath.TokenType.HOTP)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .setPeriod(PERIOD)
                .build();
        Mechanism mechanism2 = Oath.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(OTHER_ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setType(Oath.TokenType.HOTP)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .setPeriod(PERIOD)
                .build();

        assertFalse(mechanism1.equals(mechanism2));
        assertEquals(mechanism1.compareTo(mechanism2), 0);
        assertEquals(mechanism2.compareTo(mechanism1), 0);
    }

}