/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.biometric;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.forgerock.android.auth.Logger;
import org.forgerock.android.core.R;

/**
 * Headless Fragment to receive result from KeyguardManager library.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DeviceCredentialFragment extends Fragment {

    static final String TAG = DeviceCredentialFragment.class.getName();
    static final int LOCK_REQUEST_CODE = 221;

    private BiometricAuth biometricAuth;

    /**
     * Initialize the Fragment to receive device credential authentication result.
     */
    static void launch(@NonNull BiometricAuth biometricAuth) {
        FragmentManager fragmentManager = biometricAuth.getActivity().getSupportFragmentManager();
        DeviceCredentialFragment existing = (DeviceCredentialFragment) fragmentManager.findFragmentByTag(TAG);
        if (existing != null) {
            existing.biometricAuth = null;
            fragmentManager.beginTransaction().remove(existing).commitNow();
        }

        DeviceCredentialFragment fragment = new DeviceCredentialFragment();
        fragment.biometricAuth = biometricAuth;
        fragmentManager.beginTransaction().add(fragment, DeviceCredentialFragment.TAG).commit();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String title = this.biometricAuth.getTitle() != null
                ? this.biometricAuth.getTitle()
                : getString(R.string.biometric_title);
        String reason = this.biometricAuth.getSubtitle() != null
                ? this.biometricAuth.getSubtitle()
                : getString(R.string.biometric_subtitle);

        Intent authIntent = biometricAuth.getKeyguardManager()
                .createConfirmDeviceCredentialIntent(title, reason);

        startActivityForResult(authIntent, LOCK_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commitNow();
        }
        if (requestCode == LOCK_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                this.biometricAuth.getBiometricAuthListener().onAuthenticationSucceeded(null);
            } else {
                Logger.debug(TAG, "Fail to approve using device Credentials. requestCode " +
                        "is %s", resultCode);
                this.biometricAuth.getBiometricAuthListener().onAuthenticationError(
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                        "Fail to approve using device Credentials.");
            }
        }
        this.biometricAuth = null;
    }

}
