/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import android.content.Context;

import org.forgerock.android.auth.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * After the device is rooted, the super user may change permission of some files.
 */
public class PermissionDetector implements RootDetector {

    public static final String[] NOT_WRITABLE_PATH = {
            "/system",
            "/system/bin",
            "/system/sbin",
            "/system/xbin",
            "/vendor/bin",
            "/sbin",
            "/etc",
    };
    private static final String TAG = PermissionDetector.class.getName();

    @Override
    public double isRooted(Context context) {

        String[] lines = mountReader();

        int sdkVersion = android.os.Build.VERSION.SDK_INT;

        for (String line : lines) {

            String[] args = line.split(" ");

            if ((sdkVersion <= android.os.Build.VERSION_CODES.M && args.length < 4)
                    || (sdkVersion > android.os.Build.VERSION_CODES.M && args.length < 6)) {
                Logger.error(TAG, "Error formatting mount line: " + line);
                continue;
            }

            String mountPoint;
            String mountOptions;

            if (sdkVersion > android.os.Build.VERSION_CODES.M) {
                mountPoint = args[2];
                mountOptions = args[5];
            } else {
                mountPoint = args[1];
                mountOptions = args[3];
            }

            for (String pathToCheck : NOT_WRITABLE_PATH) {
                if (mountPoint.equalsIgnoreCase(pathToCheck)) {

                    if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.M) {
                        mountOptions = mountOptions.replace("(", "");
                        mountOptions = mountOptions.replace(")", "");
                    }

                    for (String option : mountOptions.split(",")) {
                        if (option.equalsIgnoreCase("rw")) {
                            return 1.0;
                        }
                    }
                }
            }
        }
        return 0;
    }

    private String[] mountReader() {
        try {
            InputStream inputstream = Runtime.getRuntime().exec("mount").getInputStream();
            if (inputstream == null) return new String[]{};
            String propVal = new Scanner(inputstream).useDelimiter("\\A").next();
            return propVal.split("\n");
        } catch (IOException | NoSuchElementException e) {
            return new String[]{};
        }
    }
}
