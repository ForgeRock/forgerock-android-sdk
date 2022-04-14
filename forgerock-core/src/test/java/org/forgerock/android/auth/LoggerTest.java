/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public class LoggerTest {

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
        Logger.set(Logger.Level.DEBUG);
        ShadowLog.clear();
    }

    @Test
    public void testLowerLevel() {
        Logger.set(Logger.Level.ERROR);
        Logger.warn("Test", "warning");
        Logger.debug("Test", "debugging");
        //Should not log
        assertEquals(0, ShadowLog.getLogs().size());
    }

    @Test
    public void testHigherLevel() {
        Logger.set(Logger.Level.DEBUG);
        Logger.warn("Test", "warning");
        Logger.error("Test", "error");
        assertEquals(2, ShadowLog.getLogs().size());
    }

    @Test
    public void testNone() {
        Logger.set(Logger.Level.NONE);
        Logger.debug("Test", "debugging");
        Logger.warn("Test", "warning");
        Logger.debug("Test", "debugging");
        assertEquals(0, ShadowLog.getLogs().size());
    }

    @Test
    public void testIsDebugEnabled() {
        assertTrue(Logger.isDebugEnabled());
        Logger.set(Logger.Level.NONE);
        assertFalse(Logger.isDebugEnabled());
    }
}
