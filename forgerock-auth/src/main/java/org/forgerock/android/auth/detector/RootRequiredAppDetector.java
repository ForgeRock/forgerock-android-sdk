/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import android.content.Context;

/**
 * Check if root required App are installed
 */
public class RootRequiredAppDetector extends PackageDetector {

    private static final String[] CURRENT_KNOWN_APPS_REQUIRE_ROOT = {
            "com.koushikdutta.rommanager",
            "com.koushikdutta.rommanager.license",
            "com.dimonvideo.luckypatcher",
            "com.chelpus.lackypatch",
            "com.ramdroid.appquarantine",
            "com.ramdroid.appquarantinepro",
            "com.android.vending.billing.InAppBillingService.COIN",
            "com.chelpus.luckypatcher"
    };

    @Override
    public double isRooted(Context context) {
        if (super.isRooted(context) > 0) {
            return 0.5d;
        }
        return 0;
    }

    @Override
    protected String[] getPackages() {
        return CURRENT_KNOWN_APPS_REQUIRE_ROOT;
    }


}
