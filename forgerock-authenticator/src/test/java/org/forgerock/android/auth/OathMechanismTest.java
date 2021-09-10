/*
 * Copyright (c) 2020 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.MechanismCreationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class OathMechanismTest extends FRABaseTest {

    @Test
    public void testShouldParseHOTPToJsonSuccessfully() throws MechanismCreationException {
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

        OathMechanism mechanism = HOTPMechanism.builder()
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
    public void testShouldParseTOTPToJsonSuccessfully() throws MechanismCreationException {
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

        OathMechanism mechanism = TOTPMechanism.builder()
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

}