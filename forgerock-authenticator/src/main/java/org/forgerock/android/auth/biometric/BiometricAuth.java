/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.biometric;

import static android.content.Context.KEYGUARD_SERVICE;
import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import org.forgerock.android.auth.Logger;

import java.util.concurrent.Executor;

/**
 * Helper class for managing Biometric Authentication Process.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BiometricAuth {

    private BiometricManager biometricManager;
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;

    private BiometricPrompt.PromptInfo promptInfo;

    private final String title;
    private final String subtitle;
    private final boolean allowDeviceCredentials;
    private final FragmentActivity activity;
    private final BiometricAuthCompletionHandler biometricAuthListener;

    public static final int ERROR_NO_BIOMETRICS = -1;
    public static final int ERROR_NO_DEVICE_CREDENTIAL = -2;

    private static final String TAG = BiometricAuth.class.getSimpleName();

    /**
     * Initializes Biometric authentication with the caller and callback handlers.
     * If device has no Biometric enrolled, it fallback to device credentials authentication.
     * @param title the title to be displayed on the prompt.
     * @param subtitle the subtitle to be displayed on the prompt.
     * @param allowDeviceCredentials if {@code true}, accepts device PIN, pattern, or password to process notification.
     * @param activity the activity of the client application that will host the prompt.
     * @param biometricAuthListener listener for receiving the biometric authentication result.
     */
    public BiometricAuth(String title,
                         String subtitle,
                         boolean allowDeviceCredentials,
                         @NonNull FragmentActivity activity,
                         @NonNull BiometricAuthCompletionHandler biometricAuthListener) {
        this.title = title;
        this.subtitle = subtitle;
        this.allowDeviceCredentials = allowDeviceCredentials;
        this.activity = activity;
        this.biometricAuthListener = biometricAuthListener;

        setServicesFromActivity(activity);
    }

    /**
     * Return the mechanism used to lock and unlock the device.
     * @return the KeyguardManager instance.
     */
    public KeyguardManager getKeyguardManager() {
        return keyguardManager;
    }

    /**
     * The title to be displayed on the prompt.
     * @return the Title as String.
     */
    public String getTitle() {
        return title;
    }

    /**
     * The subtitle to be displayed on the prompt.
     * @return the Subtitle as String.
     */
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * Return the Biometric Authentication listener used to return authentication result.
     * @return the Biometric Authentication listener.
     */
    public BiometricAuthCompletionHandler getBiometricAuthListener() {
        return biometricAuthListener;
    }

    /**
     * Return the activity of the client application that will host the prompt.
     * @return the Activity that hosts the prompt.
     */
    public FragmentActivity getActivity() {
        return activity;
    }

    /*
     * Starts authentication process.
     */
    public void authenticate() {
        // if biometric only, try biometric prompt
        if(!this.allowDeviceCredentials) {
            if(hasBiometricCapability()) {
                initBiometricAuthentication();
            } else {
                Logger.debug(TAG, "allowDeviceCredentials is set to false, but no biometric " +
                        "hardware found or enrolled.");
                this.biometricAuthListener.onError(ERROR_NO_BIOMETRICS, "It requires " +
                        "biometric authentication. No biometric hardware found or enrolled.");
            }
            return;
        }

        // API 29 and above, use BiometricPrompt
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            initBiometricAuthentication();
            return;
        }

        // API 23 - 28, check enrollment with FingerprintManager once BiometricPrompt might not work
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && fingerprintManager != null
                && fingerprintManager.hasEnrolledFingerprints()) {
                initBiometricAuthentication();
            return;
        }

        // API 23 or higher, no biometric, fallback to device credentials
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && this.keyguardManager != null
                && this.keyguardManager.isDeviceSecure()) {
            initDeviceCredentialAuthentication();
        } else {
            Logger.debug(TAG, "This device does not support required security features." +
                    " No Biometric, device PIN, pattern, or password registered.");
            this.biometricAuthListener.onError(ERROR_NO_DEVICE_CREDENTIAL, "This device does " +
                    "not support required security features. No Biometric, device PIN, pattern, " +
                    "or password registered.");
        }
    }

    private void setServicesFromActivity(@NonNull FragmentActivity activity) {
        Context context = activity.getBaseContext();
        this.biometricManager = BiometricManager.from(activity);
        this.keyguardManager = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        }
    }

    private BiometricPrompt initBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this.activity);
        BiometricPrompt biometricPrompt = new BiometricPrompt(
                this.activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                Logger.debug(TAG, "Authentication failed. errCode is %s", errorCode);
                biometricAuthListener.onError(errorCode, errString.toString());
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                Logger.debug(TAG, "Authentication was successful");
                biometricAuthListener.onSuccess(result);
            }

            @Override
            public void onAuthenticationFailed() {
                // This is usually called when a biometric (e.g. fingerprint, face, etc.) is
                // presented but not recognized as belonging to the user.
                Logger.debug(TAG, "Biometric authentication failed for unknown reason.");
            }
        });

        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(this.title != null ? this.title : "Biometric Authentication for login")
                .setSubtitle(this.subtitle != null ? this.subtitle : "Log in using your biometric credential");

        int authenticators;
        if(this.allowDeviceCredentials) {
            authenticators = BIOMETRIC_WEAK | DEVICE_CREDENTIAL;
        } else {
            authenticators = BIOMETRIC_WEAK;
            builder.setNegativeButtonText("Cancel");
        }
        builder.setAllowedAuthenticators(authenticators);

        this.promptInfo = builder.build();

        return biometricPrompt;
    }

    private boolean hasBiometricCapability() {
        if (this.biometricManager == null) return false;
        int canAuthenticate = this.biometricManager.canAuthenticate(BIOMETRIC_WEAK);
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void initBiometricAuthentication() {
        BiometricPrompt biometricPrompt = initBiometricPrompt();
        biometricPrompt.authenticate(this.promptInfo);
    }

    private void initDeviceCredentialAuthentication() {
        DeviceCredentialFragment.launch(this);
    }

}

