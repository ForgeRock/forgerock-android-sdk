/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.util.Log;

import org.forgerock.android.core.BuildConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLog.LogItem;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class LoggerTest {

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
        Logger.set(Logger.Level.DEBUG);
        ShadowLog.clear();
    }

    @Test
    public void testDebugLogging() {
        Logger.debug("Test", "This is a test", null);
        LogItem logItem = ShadowLog.getLogsForTag(Logger.FORGE_ROCK).get(0);
        assertEquals(Log.INFO, logItem.type);
        assertEquals("[" + BuildConfig.VERSION_NAME + "] [Test]: This is a test", logItem.msg);
    }


    @Test
    public void testDebugLoggingWithArgs() {
        Logger.debug("Test", "This is a test %s, %d", "hello", 3);
        LogItem logItem = ShadowLog.getLogsForTag(Logger.FORGE_ROCK).get(0);
        assertEquals(Log.INFO, logItem.type);
        assertEquals("[" + BuildConfig.VERSION_NAME + "] [Test]: This is a test hello, 3", logItem.msg);
    }

    @Test
    public void testLowerLevel() {
        Logger.set(Logger.Level.ERROR);
        Logger.warn("Test", "warning");
        Logger.debug("Test", "debugging");
        //Should not log
        assertEquals(0, ShadowLog.getLogsForTag(Logger.FORGE_ROCK).size());
    }

    @Test
    public void testHigherLevel() {
        Logger.set(Logger.Level.DEBUG);
        Logger.warn("Test", "warning");
        Logger.error("Test", "error");
        assertEquals(2, ShadowLog.getLogsForTag(Logger.FORGE_ROCK).size());
    }

    @Test
    public void testNone() {
        Logger.set(Logger.Level.NONE);
        Logger.debug("Test", "debugging");
        Logger.warn("Test", "warning");
        Logger.debug("Test", "debugging");
        assertEquals(0, ShadowLog.getLogsForTag(Logger.FORGE_ROCK).size());
    }


    @Test
    public void testWarnThrowable() {
        Logger.warn("Test", new IllegalArgumentException("test"), "warning");
        LogItem logItem = ShadowLog.getLogsForTag(Logger.FORGE_ROCK).get(0);
        assertEquals(Log.WARN, logItem.type);
        assertTrue(logItem.throwable instanceof IllegalArgumentException);
    }

    @Test
    public void testErrorThrowable() {
        Logger.error("Test", new IllegalArgumentException("test"), "error");
        LogItem logItem = ShadowLog.getLogsForTag(Logger.FORGE_ROCK).get(0);
        assertEquals(Log.ERROR, logItem.type);
        assertTrue(logItem.throwable instanceof IllegalArgumentException);
    }

    @Test
    public void testIsDebugEnabled() {
        assertTrue(Logger.isDebugEnabled());
        Logger.set(Logger.Level.NONE);
        assertFalse(Logger.isDebugEnabled());
    }
}
