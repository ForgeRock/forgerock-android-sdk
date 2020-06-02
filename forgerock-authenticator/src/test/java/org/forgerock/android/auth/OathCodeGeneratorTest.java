/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.OathMechanismException;
import org.forgerock.android.auth.util.TimeKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class OathCodeGeneratorTest extends FRABaseTest {

    private DefaultStorageClient storageClient;

    @Before
    public void setUp() throws IOException {
        storageClient = mock(DefaultStorageClient.class);
        given(storageClient.setMechanism(any(OathMechanism.class))).willReturn(true);
    }

    @After
    public void cleanUp() throws Exception {
        OathCodeGenerator.reset();
    }

    @Test
    public void testShouldFailToGetInstanceNotInitialized() {
        OathCodeGenerator oathCodeGenerator = null;

        try {
            oathCodeGenerator = OathCodeGenerator.getInstance();
            fail("Should throw IllegalStateException");
        } catch (Exception e) {
            assertNull(oathCodeGenerator);
            assertTrue(e instanceof IllegalStateException);
        }
    }

    @Test
    public void testHandleHOTPCorrectlyWith6Digits() throws OathMechanismException {
        HOTPMechanism oath = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        TimeKeeper timeKeeper = new TimeKeeper();
        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance(storageClient).generateNextCode(oath, timeKeeper).getCurrentCode(), "785324");
        assertEquals(OathCodeGenerator.getInstance(storageClient).generateNextCode(oath, timeKeeper).getCurrentCode(), "361422");
        assertEquals(OathCodeGenerator.getInstance(storageClient).generateNextCode(oath, timeKeeper).getCurrentCode(), "054508");

        assertEquals(oath.getCounter(), 3);
    }

    @Test
    public void testHandleHOTPCorrectlyWith8Digits() throws OathMechanismException {
        HOTPMechanism oath = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(8)
                .setCounter(COUNTER)
                .build();

        TimeKeeper timeKeeper = new TimeKeeper();
        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "60785324");
        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "92361422");
        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "38054508");

        assertEquals(oath.getCounter(), 3);
    }

    @Test
    public void testHandleTOTPCorrectly() throws OathMechanismException {

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

        TOTPMechanism oath = (TOTPMechanism) TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "994721");
        timeKeeper.timeTravel(30000);
        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "589452");
        timeKeeper.timeTravel(30000);
        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "982313");
    }

    @Test
    public void testShouldFailToHandleHOTPInvalidAlgorithm() {
        HOTPMechanism oath = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("INVALID!")
                .setSecret(SECRET)
                .setDigits(8)
                .setCounter(COUNTER)
                .build();

        TimeKeeper timeKeeper = new TimeKeeper();

        try {
            OathCodeGenerator.getInstance(storageClient).generateNextCode(oath, timeKeeper);
            fail("Should throw OathMechanismException");
        } catch (Exception e) {
            assertTrue(e instanceof OathMechanismException);
        }
    }

    @Test
    public void testShouldFailToHandleHOTPInvalidSecret() {
        HOTPMechanism oath = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm(ALGORITHM)
                .setSecret("00018977")
                .setDigits(8)
                .setCounter(COUNTER)
                .build();

        TimeKeeper timeKeeper = new TimeKeeper();

        try {
            OathCodeGenerator.getInstance(storageClient).generateNextCode(oath, timeKeeper);
            fail("Should throw OathMechanismException");
        } catch (Exception e) {
            assertTrue(e instanceof OathMechanismException);
        }
    }

}
