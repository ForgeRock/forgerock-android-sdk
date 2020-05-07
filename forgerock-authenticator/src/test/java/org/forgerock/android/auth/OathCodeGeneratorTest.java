/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.util.TimeKeeper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OathCodeGeneratorTest extends FRABaseTest {

    @Test
    public void testHandleHOTPCorrectlyWith6Digits() throws Exception {
        Oath oath = Oath.builder()
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

        TimeKeeper timeKeeper = new TimeKeeper();

        assertEquals(OathCodeGenerator.generateNextCode(oath, timeKeeper).getCurrentCode(), "785324");
        assertEquals(OathCodeGenerator.generateNextCode(oath, timeKeeper).getCurrentCode(), "361422");
        assertEquals(OathCodeGenerator.generateNextCode(oath, timeKeeper).getCurrentCode(), "054508");

        assertEquals(oath.getCounter(), 3);
    }

    @Test
    public void testHandleHOTPCorrectlyWith8Digits() throws Exception {
        Oath oath = Oath.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setType(Oath.TokenType.HOTP)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(8)
                .setCounter(COUNTER)
                .setPeriod(PERIOD)
                .build();

        TimeKeeper timeKeeper = new TimeKeeper();

        assertEquals(OathCodeGenerator.generateNextCode(oath, timeKeeper).getCurrentCode(), "60785324");
        assertEquals(OathCodeGenerator.generateNextCode(oath, timeKeeper).getCurrentCode(), "92361422");
        assertEquals(OathCodeGenerator.generateNextCode(oath, timeKeeper).getCurrentCode(), "38054508");

        assertEquals(oath.getCounter(), 3);
    }

    @Test
    public void testHandleTOTPCorrectly() throws Exception {

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

        Oath oath = Oath.builder()
                .setMechanismUID(MECHANISM_UID)
                .setIssuer(ISSUER)
                .setAccountName(ACCOUNT_NAME)
                .setType(Oath.TokenType.TOTP)
                .setAlgorithm(ALGORITHM)
                .setSecret(SECRET)
                .setDigits(DIGITS)
                .setCounter(COUNTER)
                .setPeriod(PERIOD)
                .build();

        assertEquals(OathCodeGenerator.generateNextCode(oath, timeKeeper).getCurrentCode(), "994721");
        timeKeeper.timeTravel(30000);
        assertEquals(OathCodeGenerator.generateNextCode(oath, timeKeeper).getCurrentCode(), "589452");
        timeKeeper.timeTravel(30000);
        assertEquals(OathCodeGenerator.generateNextCode(oath, timeKeeper).getCurrentCode(), "982313");

        assertEquals(oath.getCounter(), 0);
    }

}
