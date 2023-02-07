/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.policy;

import android.content.Context;

import org.forgerock.android.auth.detector.FRRootDetector;
import org.json.JSONException;

/**
 * The Device Tampering policy checks the integrity of device's software and hardware.
 */
public class DeviceTamperingPolicy extends FRAPolicy {

    private static final String DEVICE_TAMPERING_POLICY = "deviceTampering";
    private static final String SCORE_KEY = "score";

    @Override
    public String getName() {
        return DEVICE_TAMPERING_POLICY;
    }

    @Override
    public boolean evaluate(Context context) {
        try {
            double scoreThreshold;

            if(data.has(SCORE_KEY)) {
                scoreThreshold = data.getDouble(SCORE_KEY);
                return !isDeviceRooted(context, scoreThreshold);
            } else {
                return true;
            }
        } catch (JSONException e) {
            throw new RuntimeException("Error parsing deviceTampering policy data.", e);
        }
    }

    private boolean isDeviceRooted(Context context, double scoreThreshold) {
        double isRooted = FRRootDetector.DEFAULT.isRooted(context);
        return isRooted >= scoreThreshold;
    }

}
