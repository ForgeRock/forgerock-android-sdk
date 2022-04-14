/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Logger for ForgeRock SDK
 */
public class Logger {

    public enum Level {
        DEBUG,
        WARN,
        ERROR,
        NONE,
    }

    //Default level to warn
    private static Level level = Level.WARN;

    private static final Set<FRLogger> loggers = new HashSet<FRLogger>() {{
        add(new FRConsoleLogger()); // Default logger, always present
    }};

    public static void set(Level level) {
        Logger.level = level;
    }

    public static boolean isDebugEnabled() {
        return Logger.level == Level.DEBUG;
    }

    /**
     * Add a logger to be handled by the system.
     *
     * @return {@code true} if the logger was not already added
     */
    public static boolean addLogger(@NonNull FRLogger logger) {
        return loggers.add(logger);
    }

    /**
     * Remove a logger from the system.
     *
     * @return {@code true} if the logger was added previously
     */
    public static boolean removeLogger(@NonNull FRLogger logger) {
        return loggers.remove(logger);
    }

    public static void error(String tag, Throwable t, String message, Object... values) {
        if (isLoggingDisabled(Level.ERROR)) return;
        for (FRLogger logger : loggers) {
            logger.error(tag, t, message, values);
        }
    }

    public static void error(String tag, String message, Object... values) {
        if (isLoggingDisabled(Level.ERROR)) return;
        for (FRLogger logger : loggers) {
            logger.error(tag, message, values);
        }
    }

    public static void warn(String tag, String message, Object... values) {
        if (isLoggingDisabled(Level.WARN)) return;
        for (FRLogger logger : loggers) {
            logger.warn(tag, message, values);
        }
    }

    public static void warn(String tag, Throwable t, String message, Object... values) {
        if (isLoggingDisabled(Level.WARN)) return;
        for (FRLogger logger : loggers) {
            logger.warn(tag, t, message, values);
        }
    }

    public static void debug(String tag, String message, Object... values) {
        if (isLoggingDisabled(Level.DEBUG)) return;
        for (FRLogger logger : loggers) {
            logger.debug(tag, message, values);
        }
    }

    private static boolean isLoggingDisabled(Level level) {
        return level.ordinal() < Logger.level.ordinal();
    }
}
