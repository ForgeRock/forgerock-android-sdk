/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.util.Log;

import org.forgerock.android.core.BuildConfig;

/**
 * {@link FRLogger} implemented with Android's default {@link Log} utility.
 */
class FRConsoleLogger implements FRLogger {

    private static final String FORGE_ROCK = "ForgeRock";

    @Override
    public void error(String tag, Throwable t, String message, Object... values) {
        log(Logger.Level.ERROR, tag, t, message, values);
    }

    @Override
    public void error(String tag, String message, Object... values) {
        log(Logger.Level.ERROR, tag, null, message, values);
    }

    @Override
    public void warn(String tag, String message, Object... values) {
        log(Logger.Level.WARN, tag, null, message, values);
    }

    @Override
    public void warn(String tag, Throwable t, String message, Object... values) {
        log(Logger.Level.WARN, tag, t, message, values);
    }

    @Override
    public void debug(String tag, String message, Object... values) {
        log(Logger.Level.DEBUG, tag, null, message, values);
    }

    private void log(Logger.Level level, String tag, Throwable t, String message, Object... args) {
        final String value =
                String.format("[%s] [%s]: ", BuildConfig.VERSION_NAME, tag)
                        + String.format(message, args);

        switch (level) {
            case DEBUG:
                Log.i(FORGE_ROCK, value);
                break;
            case WARN:
                Log.w(FORGE_ROCK, value, t);
                break;
            case ERROR:
                Log.e(FORGE_ROCK, value, t);
                break;
            default:
                break;
        }
    }
}
