/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import android.content.Context;

public interface RootDetector {

    /**
     * Detect the device is rooted.
     *
     * @param context The application context
     * @return 0 - 1 How likely the device is rooted, 0 - not likely, 0.5 - likely, 1 - Very likely
     */
    double isRooted(Context context);
}
