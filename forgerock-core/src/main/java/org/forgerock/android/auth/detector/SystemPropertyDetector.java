/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Check system property with expected value.
 */
public abstract class SystemPropertyDetector implements RootDetector {

    private boolean exists(Map<String, String> properties) {

        String[] lines = propsReader();

        for (String line : lines) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                if (line.contains(entry.getKey()) &&
                        line.contains("[" + entry.getValue() + "]")) {
                    return true;
                }
            }
        }
        return false;
    }

    public double isRooted(Context context) {
        if (exists(getProperties())) {
            return 1.0;
        } else {
            return 0;
        }
    }

    private String[] propsReader() {
        try {
            InputStream inputstream = Runtime.getRuntime().exec("getprop").getInputStream();
            if (inputstream == null) return new String[]{};
            String propVal = new Scanner(inputstream).useDelimiter("\\A").next();
            return propVal.split("\n");
        } catch (IOException | NoSuchElementException e) {
            return new String[]{};
        }
    }

    protected abstract Map<String, String> getProperties();


}
