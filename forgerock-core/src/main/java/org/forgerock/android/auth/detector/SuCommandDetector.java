/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.detector;

/**
 * Check su command exists
 */
public class SuCommandDetector extends CommandDetector {

    @Override
    protected String[] getCommands() {
        return new String[]{"su"};
    }
}
