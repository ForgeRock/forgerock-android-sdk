/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.collector.FRDeviceCollector;
import org.json.JSONObject;

public class FRDevice {

    private static final FRDevice INSTANCE = new FRDevice();

    private FRDevice() {
    }

    public static FRDevice getInstance() {
        return INSTANCE;
    }

    /**
     * Retrieve the Device Profile.
     *
     * @param listener Listener to listen for the device profile result.
     */
    public void getProfile(FRListener<JSONObject> listener) {
        FRDeviceCollector.DEFAULT.collect(Config.getInstance().getContext(), listener);
    }
}
