/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.util.Log;
import androidx.annotation.VisibleForTesting;
import org.forgerock.android.core.BuildConfig;

/**
 * Logger for ForgeRock SDK
 */
public class Logger {

    @VisibleForTesting
    static final String FORGE_ROCK = "ForgeRock";

    public enum Level {
        DEBUG,
        WARN,
        ERROR,
        NONE,
    }

    //Default level to warn
    private static Level level = Level.WARN;

    public static void set(Level level) {
        Logger.level = level;
    }

    public static boolean isDebugEnabled() {
        return Logger.level == Level.DEBUG;
    }

    private static void log(Level level, String tag, Throwable t, String message, Object... args) {
        if (level.ordinal() >= Logger.level.ordinal() ) {
            String value =
                    String.format("[%s] [%s]: ", BuildConfig.VERSION_NAME, tag)
                            + String.format(message, args);
            switch (level) {
                case DEBUG:
                    Log.i(FORGE_ROCK, value);
                    return;
                case WARN:
                    Log.w(FORGE_ROCK, value, t);
                    return;
                case ERROR:
                    Log.e(FORGE_ROCK, value, t);
            }
        }
    }

    public static void error(String tag, Throwable t, String message, Object... values) {
        log(Level.ERROR, tag, t, message, values);
    }

    public static void error(String tag, String message, Object... values) {
        log(Level.ERROR, tag, null, message, values);
    }

    public static void warn(String tag, String message, Object... values) {
        log(Level.WARN, tag, null, message, values);
    }

    public static void warn(String tag, Throwable t, String message, Object... values) {
        log(Level.WARN, tag, t, message, values);
    }

    public static void debug(String tag, String message, Object... values) {
        log(Level.DEBUG, tag, null, message, values);
    }
}
