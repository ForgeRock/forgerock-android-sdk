/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Check command exists
 */
public abstract class CommandDetector implements RootDetector {

    private boolean exists(String command) {

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "which", command });
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return in.readLine() != null;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }

    }

    @Override
    public double isRooted(Context context) {

        for (String command: getCommands()) {
            if (exists(command)) {
                return 1.0;
            }
        }
        return 0;
   }

    protected abstract String[] getCommands();

}
