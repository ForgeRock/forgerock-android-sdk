/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.policy;

import android.content.Context;

import org.forgerock.android.auth.biometric.BiometricAuth;

/**
 * The Biometric Available policy checks if the device has enabled Biometric capabilities and
 * lock screen security is enabled.
 *
 *  JSON Policy format:
 *  {"biometricAvailable": { }}
 */
public class BiometricAvailablePolicy extends FRAPolicy {

    private static final String BIOMETRIC_AVAILABLE_POLICY = "biometricAvailable";

    @Override
    public String getName() {
        return BIOMETRIC_AVAILABLE_POLICY;
    }

    @Override
    public boolean evaluate(Context context) {
        return BiometricAuth.isBiometricAvailable(context);
    }

}
