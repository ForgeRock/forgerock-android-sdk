/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

/**
 * Check if there are well-known root cloaking App installed.
 */
public class RootCloakingAppDetector extends PackageDetector {

    private static final String[] CURRENT_ROOT_CLOAKING_APPS = {
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloakplus",
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",
            "com.zachspong.temprootremovejb",
            "com.amphoras.hidemyroot",
            "com.amphoras.hidemyrootadfree",
            "com.formyhm.hiderootPremium",
            "com.formyhm.hideroot"
    };

    @Override
    protected String[] getPackages() {
        return CURRENT_ROOT_CLOAKING_APPS;
    }


}
