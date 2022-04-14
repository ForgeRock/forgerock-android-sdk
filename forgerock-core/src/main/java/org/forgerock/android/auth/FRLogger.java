/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * General use logging interface that can be managed by {@link Logger}.
 */
public interface FRLogger {

    void error(String tag, Throwable t, String message, Object... values);

    void error(String tag, String message, Object... values);

    void warn(String tag, String message, Object... values);

    void warn(String tag, Throwable t, String message, Object... values);

    void debug(String tag, String message, Object... values);
}
