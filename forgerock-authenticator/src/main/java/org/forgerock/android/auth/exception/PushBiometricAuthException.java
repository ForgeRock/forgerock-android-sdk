/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

import androidx.annotation.NonNull;

/**
 * Exception representing a failure during biometric authentication, providing error code and message.
 * <p>
 * Error codes correspond to BiometricPrompt error codes:
 * <ul>
 *   <li>ERROR_HW_UNAVAILABLE</li>
 *   <li>ERROR_UNABLE_TO_PROCESS</li>
 *   <li>ERROR_TIMEOUT</li>
 *   <li>ERROR_NO_SPACE</li>
 *   <li>ERROR_CANCELED</li>
 *   <li>ERROR_LOCKOUT</li>
 *   <li>ERROR_LOCKOUT_PERMANENT</li>
 *   <li>ERROR_USER_CANCELED</li>
 *   <li>ERROR_NO_BIOMETRICS</li>
 *   <li>ERROR_HW_NOT_PRESENT</li>
 *   <li>ERROR_NEGATIVE_BUTTON</li>
 *   <li>ERROR_VENDOR</li>
 *   <li>ERROR_SECURITY_UPDATE_REQUIRED</li>
 * </ul>
 * See AndroidX BiometricPrompt documentation for details.
 */
public class PushBiometricAuthException extends PushMechanismException {
    private final int errorCode;

    public PushBiometricAuthException(int errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code from BiometricPrompt.
     */
    public int getErrorCode() {
        return errorCode;
    }
}
