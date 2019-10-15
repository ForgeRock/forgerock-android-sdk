/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

/**
 * Check if there are well-known root App are installed.
 */
public class RootAppDetector extends PackageDetector {

    private static final String[] CURRENT_KNOWN_ROOT_APPS = {
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk"
    };

    @Override
    protected String[] getPackages() {
        return CURRENT_KNOWN_ROOT_APPS;
    }


}
