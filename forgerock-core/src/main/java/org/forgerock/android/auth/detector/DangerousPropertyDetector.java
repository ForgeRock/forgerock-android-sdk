/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import java.util.HashMap;
import java.util.Map;

/**
 * Check System properties
 */
public class DangerousPropertyDetector extends SystemPropertyDetector {

    @Override
    protected Map<String, String> getProperties() {
        final Map<String, String> dangerousProps = new HashMap<>();
        dangerousProps.put("ro.debuggable", "1");
        dangerousProps.put("ro.secure", "0");
        return dangerousProps;
    }
}
