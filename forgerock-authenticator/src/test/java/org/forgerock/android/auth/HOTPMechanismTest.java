/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.OathMechanismException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class HOTPMechanismTest extends FRABaseTest {

    private DefaultStorageClient storageClient;

    @Before
    public void setUp() {
        storageClient = mock(DefaultStorageClient.class);
        OathCodeGenerator.getInstance(storageClient);
    }

    @Test
    public void testCreateHOTPMechanism() {
        OathMechanism mechanism = HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        assertEquals(mechanism.getMechanismUID(), MECHANISM_UID);
        assertEquals(mechanism.getIssuer(), ISSUER);
        assertEquals(mechanism.getAccountName(), ACCOUNT_NAME);
        assertEquals(mechanism.getType(), Mechanism.OATH);
        assertEquals(mechanism.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(mechanism.getAlgorithm(), ALGORITHM);
        assertEquals(mechanism.getSecret(), SECRET);
        assertEquals(mechanism.getDigits(), DIGITS);
        assertEquals(((HOTPMechanism)mechanism).getCounter(), COUNTER);
    }

    @Test
    public void testShouldBeEqualEquivalentHOTPMechanism() {
        Mechanism mechanism1 = HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();
        Mechanism mechanism2 = HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        assertEquals(mechanism1, mechanism2);
        assertTrue(mechanism1.matches(mechanism1));
        assertEquals(mechanism1.hashCode(), mechanism2.hashCode());
    }

    @Test
    public void testShouldNotBeEqualDifferentHOTPMechanismWithAccountName() {
        Mechanism mechanism1 = HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();
        Mechanism mechanism2 = HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(OTHER_ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        assertFalse(mechanism1.equals(mechanism2));
        assertFalse(mechanism1.matches(mechanism2));
    }

    @Test
    public void testShouldNotBeEqualDifferentHOTPMechanismWithAccountIssuer() {
        Mechanism mechanism1 = HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();
        Mechanism mechanism2 = HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(OTHER_ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        assertFalse(mechanism1.equals(mechanism2));
        assertFalse(mechanism1.matches(mechanism2));
    }

    @Test
    public void shouldHandleHOTPCorrectlyWith6Digits() throws OathMechanismException {
        OathMechanism oath = HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        assertEquals(oath.getOathTokenCode().getCurrentCode(), "785324");
        assertEquals(oath.getOathTokenCode().getCurrentCode(), "361422");
        assertEquals(oath.getOathTokenCode().getCurrentCode(), "054508");

        assertEquals(((HOTPMechanism)oath).getCounter(), 3);
    }

    @Test
    public void shouldHandleHOTPCorrectlyWith8Digits() throws OathMechanismException {
        OathMechanism oath = HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(8)
                .setCounter(COUNTER)
                .build();

        assertEquals(oath.getOathTokenCode().getCurrentCode(), "60785324");
        assertEquals(oath.getOathTokenCode().getCurrentCode(), "92361422");
        assertEquals(oath.getOathTokenCode().getCurrentCode(), "38054508");

        assertEquals(((HOTPMechanism)oath).getCounter(), 3);
    }

    @Test
    public void testShouldParseToJsonSuccessfully() {
        String json = "{" +
                "\"id\":\"issuer1-user1-otpauth\"," +
                "\"issuer\":\"issuer1\"," +
                "\"accountName\":\"user1\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"secret\":\"REMOVED\"," +
                "\"type\":\"otpauth\"," +
                "\"oathType\":\"HOTP\"," +
                "\"algorithm\":\"sha1\"," +
                "\"digits\":6," +
                "\"counter\":\"REMOVED\"" +
                "}";

        HOTPMechanism mechanism = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        String mechanismAsJson = mechanism.toJson();

        assertNotNull(mechanismAsJson);
        assertEquals(json, mechanismAsJson);
    }

    @Test
    public void testShouldSerializeSuccessfully() {
        String json = "{" +
                "\"id\":\"issuer1-user1-otpauth\"," +
                "\"issuer\":\"issuer1\"," +
                "\"accountName\":\"user1\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"secret\":\"JMEZ2W7D462P3JYBDG2HV7PFBM\"," +
                "\"type\":\"otpauth\"," +
                "\"oathType\":\"HOTP\"," +
                "\"algorithm\":\"sha1\"," +
                "\"digits\":6," +
                "\"counter\":0" +
                "}";

        HOTPMechanism mechanism = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        String mechanismAsJson = mechanism.serialize();

        assertNotNull(mechanismAsJson);
        assertEquals(json, mechanismAsJson);
    }

    @Test
    public void testShouldDeserializeSuccessfully() {
        String json = "{" +
                "\"id\":\"issuer1-user1-otpauth\"," +
                "\"issuer\":\"issuer1\"," +
                "\"accountName\":\"user1\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"secret\":\"JMEZ2W7D462P3JYBDG2HV7PFBM\"," +
                "\"type\":\"otpauth\"," +
                "\"oathType\":\"HOTPMechanism\"," +
                "\"algorithm\":\"sha1\"," +
                "\"digits\":6," +
                "\"counter\":0" +
                "}";

        HOTPMechanism mechanism = HOTPMechanism.deserialize(json);

        assertNotNull(mechanism);
        assertEquals(mechanism.getMechanismUID(), MECHANISM_UID);
        assertEquals(mechanism.getIssuer(), ISSUER);
        assertEquals(mechanism.getAccountName(), ACCOUNT_NAME);
        assertEquals(mechanism.getType(), Mechanism.OATH);
        assertEquals(mechanism.getOathType(), OathMechanism.TokenType.HOTP);
        assertEquals(mechanism.getAlgorithm(), ALGORITHM);
        assertEquals(mechanism.getSecret(), SECRET);
        assertEquals(mechanism.getDigits(), DIGITS);
        assertEquals(mechanism.getCounter(), COUNTER);
    }

}