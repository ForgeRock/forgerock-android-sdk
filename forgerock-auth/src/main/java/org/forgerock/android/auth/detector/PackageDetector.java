/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * User Package Manager and see if application is installed.
 */
public abstract class PackageDetector implements RootDetector {

    boolean exists(Context context, String[] packages) {

        PackageManager packageManager = context.getPackageManager();

        for (String packageName : packages) {
            try {
                packageManager.getPackageInfo(packageName, 0);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                //ignore
            }
        }

        return false;
    }

    @Override
    public double isRooted(Context context) {
        if (exists(context, getPackages())) {
            return 1.0;
        } else {
            return 0;
        }
    }

    protected abstract String[] getPackages();


}
