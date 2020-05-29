/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.util;

import androidx.annotation.VisibleForTesting;

/**
 * Class used to wrap timekeeping functionality. By default uses normal methods, but can be overridden
 * to provide additional functionality.
 */
public class TimeKeeper {

    /**
     * Get the current time in milliseconds.
     * @return The current time in milliseconds.
     */
    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Throws a RuntimeException by default. Should never be used by non testing code.
     * Allows a test to simulate the passage of time, in a manner appropriate to the test.
     * @param addTime The amount of time to travel by.
     */
    @VisibleForTesting
    public void timeTravel(long addTime) {
        throw new RuntimeException("This should not be called from non test code.");
    }

}
