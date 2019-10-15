/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import android.content.Context;

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

    @Override
    public double isRooted(Context context) {

        String[] lines = mountReader();

        for (String line : lines) {
            String[] args = line.split(" ");

            if (args.length < 4){
                continue;
            }

            String mountPoint = args[1];
            String mountOptions = args[3];

            for(String pathToCheck: NOT_WRITABLE_PATH) {
                if (mountPoint.equalsIgnoreCase(pathToCheck)) {
                    for (String option : mountOptions.split(",")){
                        if (option.equalsIgnoreCase("rw")){
                            return 1;
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
