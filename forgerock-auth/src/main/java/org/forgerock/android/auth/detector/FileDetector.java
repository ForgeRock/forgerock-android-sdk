/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import android.content.Context;

import java.io.File;

/**
 * Check file exists in predefined path
 */
public abstract class FileDetector implements RootDetector {

    protected static final String[] PATHS = {
            "/data/local/",
            "/data/local/bin/",
            "/data/local/xbin/",
            "/sbin/",
            "/su/bin/",
            "/system/bin/",
            "/system/bin/.ext/",
            "/system/bin/failsafe/",
            "/system/sd/xbin/",
            "/system/usr/we-need-root/",
            "/system/xbin/",
            "/cache/",
            "/data/",
            "/dev/"
    };


    private boolean exists(String filename) {

        boolean result = false;

        for (String path : PATHS) {
            File f = new File(path, filename);
            boolean fileExists = f.exists();
            if (fileExists) {
                result = true;
            }
        }

        return result;
    }

    @Override
    public double isRooted(Context context) {

        for (String filename: getFilenames()) {
            if (exists(filename)) {
                return 1;
            }
        }
        return 0;
   }

    protected abstract String[] getFilenames();

}
