/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import org.forgerock.android.core.BuildConfig;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Logger for ForgeRock SDK
 */
public class Logger {

    @VisibleForTesting
    static final String FORGE_ROCK = "ForgeRock";

    public enum Level {
        DEBUG, WARN, ERROR, NONE
    }

    //Default level to warn
    private static Level level = Level.WARN;

    private static LoggerInterface frLogger = new FRLogger();

    public static HttpLoggingInterceptor getCustomInterceptor() {
        HttpLoggingInterceptor.Logger logger = s -> {
            frLogger.network("", s, null, null);
        };
        HttpLoggingInterceptor httpInterceptor = new HttpLoggingInterceptor(logger);
        return httpInterceptor;
    }


    public static void set(Level level) {
        Logger.level = level;
    }

    public static void setCustomLogger(LoggerInterface logger) {
        frLogger = logger;
    }

    public static boolean isDebugEnabled() {
        return Logger.level == Level.DEBUG;
    }


    public static void error(String tag, Throwable t, String message, Object... values) {
        frLogger.error(tag, t, message, values);
    }

    public static void error(String tag, String message, Object... values) {
        frLogger.error(tag, null, message, values);
    }

    public static void warn(String tag, String message, Object... values) {
        frLogger.warn(tag, null, message, values);
    }

    public static void warn(String tag, Throwable t, String message, Object... values) {
        frLogger.warn(tag, t, message, values);
    }

    public static void debug(String tag, String message, Object... values) {
        frLogger.debug(tag, null, message, values);
    }
}
