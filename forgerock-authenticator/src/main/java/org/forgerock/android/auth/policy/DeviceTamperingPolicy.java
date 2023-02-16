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
 * The Device Tampering policy checks the integrity of device's software and hardware.  It uses the
 * {@link FRRootDetector} available in the 'forgerock-core' module. The class analyzes the device by using multiple
 * device tamper detectors and returns the highest score in the range between 0.0 to 1.0 from all the
 * detectors, indicating how likely the device is rooted: 0.0 - not likely, 0.5 - likely, 1.0 -very likely.
 *
 * The policy receives the `score` value as parameter to use as threshold on determine if the device
 * is tampered. If the parameter is not passed, the policy will use the DEFAULT_THRESHOLD_SCORE.
 *
 *  JSON Policy format:
 *  {"deviceTampering": {"score": 0.8}}
 */
public class DeviceTamperingPolicy extends FRAPolicy {

    private static final String DEVICE_TAMPERING_POLICY = "deviceTampering";
    private static final String SCORE_KEY = "score";

    public static final double DEFAULT_THRESHOLD_SCORE = 1.0;

    @Override
    public String getName() {
        return DEVICE_TAMPERING_POLICY;
    }

    @Override
    public boolean evaluate(Context context) {
        try {
            double thresholdScore;

            if(this.data != null && this.data.has(SCORE_KEY)) {
                thresholdScore = this.data.getDouble(SCORE_KEY);
            } else {
                thresholdScore = DEFAULT_THRESHOLD_SCORE;
            }

            return !isDeviceRooted(context, thresholdScore);
        } catch (JSONException e) {
            throw new RuntimeException("Error parsing deviceTampering policy data.", e);
        }
    }

    private boolean isDeviceRooted(Context context, double thresholdScore) {
        double rootedScore = FRRootDetector.DEFAULT.isRooted(context);
        return rootedScore >= thresholdScore;
    }

}
