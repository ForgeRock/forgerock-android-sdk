/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.forgerock.android.auth.exception.AccountLockException;
import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.exception.OathMechanismException;
import org.forgerock.android.auth.util.TimeKeeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class OathCodeGeneratorTest extends FRABaseTest {

    private DefaultStorageClient storageClient;

    @Before
    public void setUp() throws IOException {
        storageClient = mock(DefaultStorageClient.class);
        given(storageClient.setMechanism(any(OathMechanism.class))).willReturn(true);
    }

    @After
    public void cleanUp() {
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
    public void testShouldGenerateCodeHotpSHA1Algorithm()
            throws OathMechanismException, MechanismCreationException, AccountLockException {
        HOTPMechanism oath = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("sha1")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        TimeKeeper timeKeeper = new TimeKeeper();
        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance(storageClient).generateNextCode(oath, timeKeeper).getCurrentCode(), "785324");
        assertEquals(oath.getCounter(), 1);
    }

    @Test
    public void testShouldGenerateCodeTotpSHA1Algorithm()
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

        TOTPMechanism oath = (TOTPMechanism) TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("sha1")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "994721");
    }

    @Test
    public void testShouldGenerateCodeHotpSHA224Algorithm()
            throws OathMechanismException, MechanismCreationException, AccountLockException {
        HOTPMechanism oath = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("sha224")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        TimeKeeper timeKeeper = new TimeKeeper();
        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance(storageClient).generateNextCode(oath, timeKeeper).getCurrentCode(), "137593");
        assertEquals(oath.getCounter(), 1);
    }

    @Test
    public void testShouldGenerateCodeTotpSHA224Algorithm()
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

        TOTPMechanism oath = (TOTPMechanism) TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("sha224")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "208779");
    }

    @Test
    public void testShouldGenerateCodeHotpSHA256Algorithm()
            throws OathMechanismException, MechanismCreationException, AccountLockException {
        HOTPMechanism oath = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("sha256")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        TimeKeeper timeKeeper = new TimeKeeper();
        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance(storageClient).generateNextCode(oath, timeKeeper).getCurrentCode(), "617806");
        assertEquals(oath.getCounter(), 1);
    }

    @Test
    public void testShouldGenerateCodeTotpSHA256Algorithm()
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

        TOTPMechanism oath = (TOTPMechanism) TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("sha256")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "818138");
    }

    @Test
    public void testShouldGenerateCodeHotpSHA384Algorithm()
            throws OathMechanismException, MechanismCreationException, AccountLockException {
        HOTPMechanism oath = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("sha384")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        TimeKeeper timeKeeper = new TimeKeeper();
        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance(storageClient).generateNextCode(oath, timeKeeper).getCurrentCode(), "439492");
        assertEquals(oath.getCounter(), 1);
    }

    @Test
    public void testShouldGenerateCodeTotpSHA384Algorithm()
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

        TOTPMechanism oath = (TOTPMechanism) TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("sha384")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "246759");
    }

    @Test
    public void testShouldGenerateCodeHotpSHA512Algorithm()
            throws OathMechanismException, MechanismCreationException, AccountLockException {
        HOTPMechanism oath = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("sha512")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        TimeKeeper timeKeeper = new TimeKeeper();
        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance(storageClient).generateNextCode(oath, timeKeeper).getCurrentCode(), "495031");
        assertEquals(oath.getCounter(), 1);
    }

    @Test
    public void testShouldGenerateCodeTotpSHA512Algorithm()
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

        TOTPMechanism oath = (TOTPMechanism) TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("sha512")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "902184");
    }

    @Test
    public void testShouldGenerateCodeHotpMD5Algorithm()
            throws OathMechanismException, MechanismCreationException, AccountLockException {
        HOTPMechanism oath = (HOTPMechanism) HOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("md5")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .build();

        TimeKeeper timeKeeper = new TimeKeeper();
        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance(storageClient).generateNextCode(oath, timeKeeper).getCurrentCode(), "428713");
        assertEquals(oath.getCounter(), 1);
    }

    @Test
    public void testShouldGenerateCodeTotpMD5Algorithm()
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

        TOTPMechanism oath = (TOTPMechanism) TOTPMechanism.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setAlgorithm("md5")
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setPeriod(PERIOD)
                .build();

        OathCodeGenerator.getInstance(storageClient);

        assertEquals(OathCodeGenerator.getInstance().generateNextCode(oath, timeKeeper).getCurrentCode(), "593094");
    }

    @Test
    public void testHandleHOTPCorrectlyWith6Digits()
            throws OathMechanismException, MechanismCreationException, AccountLockException {
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
    public void testHandleHOTPCorrectlyWith8Digits()
            throws OathMechanismException, MechanismCreationException, AccountLockException {
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
    public void testHandleTOTPCorrectly()
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
    public void testShouldFailToHandleHOTPInvalidAlgorithm() throws MechanismCreationException {
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
    public void testShouldFailToHandleHOTPInvalidSecret() throws MechanismCreationException {
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
