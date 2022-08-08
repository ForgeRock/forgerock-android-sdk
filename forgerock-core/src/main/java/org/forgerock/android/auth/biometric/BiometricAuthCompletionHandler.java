/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */


package org.forgerock.android.auth.biometric;

import androidx.annotation.RestrictTo;
import androidx.biometric.BiometricPrompt;

/**
 * Interface to listen to Biometric Authentication callbacks.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface BiometricAuthCompletionHandler {

    /**
     * Called when the Biometric Authentication completes successfully.
     *
     * @param result the value returned
     */
    void onSuccess(BiometricPrompt.AuthenticationResult result);

    /**
     * Called when the Biometric Authentication fails to complete.
     *
     * @param errorCode    the failure code
     * @param errorMessage the message containing the failure reason
     */
    void onError(int errorCode, String errorMessage);

}
