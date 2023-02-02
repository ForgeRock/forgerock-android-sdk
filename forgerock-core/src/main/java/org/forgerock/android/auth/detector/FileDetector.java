/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

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

        for (String path : getPaths()) {
            File f = new File(path, filename);
            boolean fileExists = f.exists();
            if (fileExists) {
                result = true;
            }
        }

        return result;
    }

    private String[] getPaths() {
        ArrayList<String> paths = new ArrayList<>(Arrays.asList(PATHS));

        String sysPaths = System.getenv("PATH");

        if (sysPaths == null || "".equals(sysPaths)){
            return paths.toArray(new String[0]);
        }

        for (String path : sysPaths.split(":")){

            if (!path.endsWith("/")){
                path = path + '/';
            }

            if (!paths.contains(path)){
                paths.add(path);
            }
        }

        return paths.toArray(new String[0]);
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
