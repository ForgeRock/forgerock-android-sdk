/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Utility class for handling Strings.
 */
class StringUtils {

    private StringUtils() {
    }

    /**
     * Determines if the string is not empty.
     *
     * @param s string to test
     * @return test if the specified string is not null and not empty (i.e. is greater than zero length).
     */
    static boolean isNotEmpty(final String s) {
        return (s != null && s.length() > 0);
    }
}
