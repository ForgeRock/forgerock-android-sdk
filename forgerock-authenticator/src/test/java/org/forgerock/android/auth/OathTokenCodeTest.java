/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.MechanismCreationException;
import org.forgerock.android.auth.util.TimeKeeper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OathTokenCodeTest {

    private final String CODE = "CODE";
    private final long EXPIRY_DELAY = 30000;
    private OathTokenCode oathTokenCode;
    private TimeKeeper timeKeeper;

    @Before
    public void setUp() {
        timeKeeper = new TimeKeeper() {
            private long offset = 0;

            @Override
            public long getCurrentTimeMillis() {
                return System.currentTimeMillis() + offset;
            }

            @Override
            public void timeTravel(long addTime) {
                offset += addTime;
            }
        };
        oathTokenCode = new OathTokenCode(timeKeeper,
                CODE, System.currentTimeMillis(),
                System.currentTimeMillis() + EXPIRY_DELAY,
                OathMechanism.TokenType.TOTP);
    }

    @Test
    public void testShouldContainCorrectCode() {
        assertEquals(oathTokenCode.getOathType(), OathMechanism.TokenType.TOTP);
        assertEquals(oathTokenCode.getCurrentCode(), CODE);
    }

    @Test
    public void testShouldReportCorrectValidityBeforeAndAfterExpiry() {
        assertEquals(oathTokenCode.isValid(), true);
        timeKeeper.timeTravel(EXPIRY_DELAY);
        assertEquals(oathTokenCode.isValid(), false);
    }

    @Test
    public void testShouldReportProgressBeforeExpiry() {
        assertEquals(getCurrentProgress(), 0);
        timeKeeper.timeTravel(EXPIRY_DELAY / 4);
        assertEquals(getCurrentProgress(), 250);
        timeKeeper.timeTravel(EXPIRY_DELAY / 4);
        assertEquals(getCurrentProgress(), 500);
        timeKeeper.timeTravel(EXPIRY_DELAY / 4);
        assertEquals(getCurrentProgress(), 750);
        timeKeeper.timeTravel(EXPIRY_DELAY / 4);
        assertEquals(getCurrentProgress(), 1000);
    }

    @Test
    public void testShouldReportFullProgressAfterExpiry() {
        timeKeeper.timeTravel(EXPIRY_DELAY * 2);
        assertEquals(getCurrentProgress(), 1000);
    }

    private int getCurrentProgress() {
        long cur = timeKeeper.getCurrentTimeMillis();
        long total = oathTokenCode.getUntil() - oathTokenCode.getStart();
        long state = cur - oathTokenCode.getStart();
        int progress = (int) (state * 1000 / total);
        return progress < 1000 ? progress : 1000;
    }

}

