/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.AccountLockException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.exception.OathMechanismException;
import org.forgerock.android.auth.util.TimeKeeper;
import org.junit.Assert;
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
public class TOTPMechanismTest extends FRABaseTest {

    private DefaultStorageClient storageClient;

    @Before
    public void setUp() {
        storageClient = mock(DefaultStorageClient.class);
        OathCodeGenerator.getInstance(storageClient);
    }

    @Test
    public void testCreateTOTPMechanism() throws MechanismCreationException {
        OathMechanism mechanism = TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        assertEquals(mechanism.getMechanismUID(), MECHANISM_UID);
        assertEquals(mechanism.getIssuer(), ISSUER);
        assertEquals(mechanism.getAccountName(), ACCOUNT_NAME);
        assertEquals(mechanism.getType(), Mechanism.OATH);
        assertEquals(mechanism.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(mechanism.getAlgorithm(), ALGORITHM);
        assertEquals(mechanism.getSecret(), SECRET);
        assertEquals(mechanism.getDigits(), DIGITS);
        assertEquals(((TOTPMechanism)mechanism).getPeriod(), PERIOD);
    }

    @Test (expected = MechanismCreationException.class)
    public void testShouldFailToCreateTOTPMechanismMissingIssuer() throws MechanismCreationException {
        TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();
    }

    @Test (expected = MechanismCreationException.class)
    public void testShouldFailToCreateTOTPMechanismMissingAccountName() throws MechanismCreationException {
        TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();
    }

    @Test
    public void testShouldBeEqualEquivalentTOTPMechanism() throws MechanismCreationException {
        Mechanism mechanism1 = TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();
        Mechanism mechanism2 = TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        assertEquals(mechanism1, mechanism2);
        assertTrue(mechanism1.matches(mechanism2));
        assertEquals(mechanism1.hashCode(), mechanism2.hashCode());
    }

    @Test
    public void testShouldNotBeEqualDifferentTOTPMechanismWithAccountName() throws MechanismCreationException {
        Mechanism mechanism1 = TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();
        Mechanism mechanism2 = TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(OTHER_ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        assertFalse(mechanism1.equals(mechanism2));
        assertFalse(mechanism1.matches(mechanism2));
    }

    @Test
    public void testShouldNotBeEqualDifferentTOTPMechanismWithAccountIssuer() throws MechanismCreationException {
        Mechanism mechanism1 = TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();
        Mechanism mechanism2 = TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(OTHER_ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        assertFalse(mechanism1.equals(mechanism2));
        assertFalse(mechanism1.matches(mechanism2));
    }

    @Test
    public void testShouldFailToGetCodeDueAccountLocked() throws MechanismCreationException {
        Account account = Account.builder()
                .setAccountName(ACCOUNT_NAME)
                .setIssuer(ISSUER)
                .setLock(true)
                .build();
        OathMechanism oath = TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();
        oath.setAccount(account);

        try {
            oath.getOathTokenCode();
            Assert.fail("Should throw OathMechanismException");
        } catch (Exception e) {
            assertTrue(e instanceof AccountLockException);
            assertTrue(e.getLocalizedMessage().contains("Account is locked"));
        }
    }

    @Test
    public void testShouldHandleTOTPCorrectly()
            throws OathMechanismException, MechanismCreationException, AccountLockException {
        TimeKeeper timeKeeper = new TimeKeeper() {
            long time = 1461773681957l;
            @Override
            public long getCurrentTimeMillis() {
                return time;
            }

            @Override
            public void timeTravel(long addTime) {
                time += addTime;
            }
        };

        OathMechanism oath = TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();
        oath.setTimeKeeper(timeKeeper);

        assertEquals(oath.getOathTokenCode().getCurrentCode(), "994721");
        timeKeeper.timeTravel(30000);
        assertEquals(oath.getOathTokenCode().getCurrentCode(), "589452");
        timeKeeper.timeTravel(30000);
        assertEquals(oath.getOathTokenCode().getCurrentCode(), "982313");
    }

    @Test
    public void testShouldParseToJsonSuccessfully() throws MechanismCreationException {
        String json = "{" +
                "\"id\":\"issuer1-user1-otpauth\"," +
                "\"issuer\":\"issuer1\"," +
                "\"accountName\":\"user1\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"secret\":\"JMEZ2W7D462P3JYBDG2HV7PFBM\"," +
                "\"type\":\"otpauth\"," +
                "\"oathType\":\"TOTP\"," +
                "\"algorithm\":\"sha1\"," +
                "\"digits\":6," +
                "\"period\":30" +
                "}";

        TOTPMechanism mechanism = (TOTPMechanism) TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        String mechanismAsJson = mechanism.toJson();

        assertNotNull(mechanismAsJson);
        assertEquals(json, mechanismAsJson);
    }

    @Test
    public void testShouldSerializeSuccessfully() throws MechanismCreationException {
        String json = "{" +
                "\"id\":\"issuer1-user1-otpauth\"," +
                "\"issuer\":\"issuer1\"," +
                "\"accountName\":\"user1\"," +
                "\"mechanismUID\":\"b162b325-ebb1-48e0-8ab7-b38cf341da95\"," +
                "\"secret\":\"JMEZ2W7D462P3JYBDG2HV7PFBM\"," +
                "\"type\":\"otpauth\"," +
                "\"oathType\":\"TOTP\"," +
                "\"algorithm\":\"sha1\"," +
                "\"digits\":6," +
                "\"period\":30" +
                "}";

        TOTPMechanism mechanism = (TOTPMechanism) TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
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
                "\"oathType\":\"TOTPMechanism\"," +
                "\"algorithm\":\"sha1\"," +
                "\"digits\":6," +
                "\"period\":30" +
                "}";

        TOTPMechanism mechanism = TOTPMechanism.deserialize(json);

        assertNotNull(mechanism);
        assertEquals(mechanism.getMechanismUID(), MECHANISM_UID);
        assertEquals(mechanism.getIssuer(), ISSUER);
        assertEquals(mechanism.getAccountName(), ACCOUNT_NAME);
        assertEquals(mechanism.getType(), Mechanism.OATH);
        assertEquals(mechanism.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(mechanism.getAlgorithm(), ALGORITHM);
        assertEquals(mechanism.getSecret(), SECRET);
        assertEquals(mechanism.getDigits(), DIGITS);
        assertEquals(mechanism.getPeriod(), PERIOD);
    }

}