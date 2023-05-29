/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import android.content.Context;

import org.forgerock.android.auth.Logger;

import java.io.File;

/**
 * Check if there are well-known root apk files exist
 */
public class RootApkDetector implements RootDetector {

    private static final String TAG = RootApkDetector.class.getSimpleName();

    private static final String[] ROOT_APK = {
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/system/app/magisk.apk"
    };

    private boolean exists(String[] apks) {

        try {
            for (String path : apks) {
                if (new File(path).exists()) {
                    return true;
                }
            }
        } catch (Exception e) {
            Logger.warn(TAG, e, "Failed to check apks");
        }
        return false;
    }

    @Override
    public double isRooted(Context context) {
        if (exists(getRootApk())) {
            return 1.0;
        } else {
            return 0;
        }
    }

    protected String[] getRootApk() {
        return ROOT_APK;
    }


}
