/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator.util;

import java.util.Map;

/**
 * Class used to wrap Map functionality.
 */
public class MapUtil {

    /**
     * Get a single value from the map.
     * @return The value for the name in the map or the default value
     */
    public static String get(Map<String, String> map, String name, String defaultValue) {
        String value = map.get(name);
        return value == null ? defaultValue : value;
    }

}
