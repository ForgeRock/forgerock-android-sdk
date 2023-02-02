/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import org.forgerock.android.auth.biometric.BiometricAuth;
import org.forgerock.android.auth.detector.FRRootDetector;

/**
 * Class used to provide Security Policies enforcement functionality.
 */
public class SecurityPolicy {

    /**
     * Determinate if an {@link Account} violate their security policies.
     * @param context The context
     * @param account The account object
     * @return {@code true}, if the Account violates their security policies, {@code false}
     * otherwise.
     */
    public boolean violatePolicies(Context context, Account account) {
        return violatePolicies(context,
                account.isBiometricAuthenticationEnforced(),
                account.isDeviceTamperingDetectionEnforced(),
                account.getDeviceTamperingScoreThreshold());
    }

    /**
     * Determinate if an {@link Account} violates the Biometric Authentication policy.
     * @param context The context
     * @param account The account object
     * @return {@code true}, if the Account violates Biometric Authentication policy,
     * {@code false} otherwise.
     */
    public boolean violateBiometricAuthenticationPolicy(Context context, Account account) {
        return violateBiometricAuthenticationPolicy(context,
                account.isBiometricAuthenticationEnforced());
    }

    /**
     * Determinate if an {@link Account} violates the Device Tampering policy.
     * @param context The context
     * @param account The account object
     * @return {@code true}, if the Account violates Device Tampering policy,
     * {@code false} otherwise.
     */
    public boolean violateDeviceTamperingPolicy(Context context, Account account) {
        return violateDeviceTamperingPolicy(context, account.isDeviceTamperingDetectionEnforced(),
                account.getDeviceTamperingScoreThreshold());
    }

    boolean violatePolicies(Context context,
                            boolean enforceBiometricAuthentication,
                            boolean enforceDeviceTamperingDetection,
                            double deviceTamperingScore) {
        boolean violate = false;
        if(violateDeviceTamperingPolicy(context, enforceDeviceTamperingDetection, deviceTamperingScore)) {
            violate = true;
        } else if(violateBiometricAuthenticationPolicy(context, enforceBiometricAuthentication)) {
            violate = true;
        }
        return violate;
    }

    @VisibleForTesting
    boolean violateBiometricAuthenticationPolicy(Context context,
                                                 boolean enforceBiometricAuthentication) {
        return enforceBiometricAuthentication && !isBiometricCapable(context);
    }

    @VisibleForTesting
    boolean violateDeviceTamperingPolicy(Context context,
                                         boolean enforceTamperingDetection,
                                         double deviceTamperingScore) {
        return enforceTamperingDetection && isDeviceRooted(context, deviceTamperingScore);
    }

    @VisibleForTesting
    boolean isBiometricCapable(Context context) {
        return BiometricAuth.isBiometricAvailable(context);
    }

    @VisibleForTesting
    boolean isDeviceRooted(Context context, double scoreThreshold) {
        double isRooted = FRRootDetector.DEFAULT.isRooted(context);
        return isRooted >= scoreThreshold;
    }

}
