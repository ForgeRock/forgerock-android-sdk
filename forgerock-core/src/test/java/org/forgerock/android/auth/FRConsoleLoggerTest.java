/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import org.forgerock.android.core.BuildConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public class FRConsoleLoggerTest {

    private final FRConsoleLogger logger = new FRConsoleLogger();

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
        ShadowLog.clear();
    }

    @Test
    public void testDebugLogging() {
        logger.debug("Test", "This is a test", null);
        ShadowLog.LogItem logItem = ShadowLog.getLogs().get(0);
        assertEquals(Log.INFO, logItem.type);
        assertEquals("[" + BuildConfig.VERSION_NAME + "] [Test]: This is a test", logItem.msg);
    }

    @Test
    public void testDebugLoggingWithArgs() {
        logger.debug("Test", "This is a test %s, %d", "hello", 3);
        ShadowLog.LogItem logItem = ShadowLog.getLogs().get(0);
        assertEquals(Log.INFO, logItem.type);
        assertEquals("[" + BuildConfig.VERSION_NAME + "] [Test]: This is a test hello, 3", logItem.msg);
    }

    @Test
    public void testWarn() {
        logger.warn("Test", "This is a test");
        ShadowLog.LogItem logItem = ShadowLog.getLogs().get(0);
        assertEquals(Log.WARN, logItem.type);
        assertEquals("[" + BuildConfig.VERSION_NAME + "] [Test]: This is a test", logItem.msg);
    }

    @Test
    public void testWarnWithArgs() {
        logger.warn("Test", "This is a test %s, %d", "hello", 3);
        ShadowLog.LogItem logItem = ShadowLog.getLogs().get(0);
        assertEquals(Log.WARN, logItem.type);
        assertEquals("[" + BuildConfig.VERSION_NAME + "] [Test]: This is a test hello, 3", logItem.msg);
    }

    @Test
    public void testWarnThrowable() {
        logger.warn("Test", new IllegalArgumentException("test"), "warning");
        ShadowLog.LogItem logItem = ShadowLog.getLogs().get(0);
        assertEquals(Log.WARN, logItem.type);
        assertTrue(logItem.throwable instanceof IllegalArgumentException);
    }

    @Test
    public void testError() {
        logger.error("Test", "This is a test");
        ShadowLog.LogItem logItem = ShadowLog.getLogs().get(0);
        assertEquals(Log.ERROR, logItem.type);
        assertEquals("[" + BuildConfig.VERSION_NAME + "] [Test]: This is a test", logItem.msg);
    }

    @Test
    public void testErrorWithArgs() {
        logger.error("Test", "This is a test %s, %d", "hello", 3);
        ShadowLog.LogItem logItem = ShadowLog.getLogs().get(0);
        assertEquals(Log.ERROR, logItem.type);
        assertEquals("[" + BuildConfig.VERSION_NAME + "] [Test]: This is a test hello, 3", logItem.msg);
    }

    @Test
    public void testErrorThrowable() {
        logger.error("Test", new IllegalArgumentException("test"), "error");
        ShadowLog.LogItem logItem = ShadowLog.getLogs().get(0);
        assertEquals(Log.ERROR, logItem.type);
        assertTrue(logItem.throwable instanceof IllegalArgumentException);
    }
}
