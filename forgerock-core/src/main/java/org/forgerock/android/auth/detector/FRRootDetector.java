/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import android.content.Context;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Root Detector to detect device is rooted or not.
 * Most of the detectors implementation are reference from Open Source library https://github.com/scottyab/rootbeer
 */
@Builder
public class FRRootDetector implements RootDetector {

   public static final List<RootDetector> DEFAULT_DETECTORS = new ArrayList<>();

    static {
        DEFAULT_DETECTORS.add(new BuildTagsDetector());
        DEFAULT_DETECTORS.add(new DangerousPropertyDetector());
        DEFAULT_DETECTORS.add(new NativeDetector());
        DEFAULT_DETECTORS.add(new PermissionDetector());
        DEFAULT_DETECTORS.add(new RootApkDetector());
        DEFAULT_DETECTORS.add(new RootAppDetector());
        DEFAULT_DETECTORS.add(new RootCloakingAppDetector());
        DEFAULT_DETECTORS.add(new RootProgramFileDetector());
        DEFAULT_DETECTORS.add(new RootRequiredAppDetector());
        DEFAULT_DETECTORS.add(new SuCommandDetector());
        DEFAULT_DETECTORS.add(new BusyBoxProgramFileDetector());
    }

    public static final RootDetector DEFAULT = FRRootDetector.builder()
            .detectors(DEFAULT_DETECTORS).build();

    @Singular
    @Getter
    private List<RootDetector> detectors;

    @Override
    public double isRooted(Context context) {
        double max = 0;
        for (RootDetector detector : detectors) {
            max = Math.max(max, detector.isRooted(context));
            if (max >= 1) {
                return max;
            }
        }
        return max;
    }
}
