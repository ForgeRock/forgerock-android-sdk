/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

/**
 * Check common root program exist
 */
public class RootProgramFileDetector extends FileDetector {

    private static final String[] CURRENT_KNOWN_ROOT_PROGRAM = {
            "su",
            "magisk",
    };

    @Override
    protected String[] getFilenames() {
        return CURRENT_KNOWN_ROOT_PROGRAM;
    }
}
