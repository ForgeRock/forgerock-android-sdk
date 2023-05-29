/*
 * Copyright (c) 2020 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.os.Build;
import android.security.keystore.StrongBoxUnavailableException;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

import lombok.NonNull;

import static android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE;

/**
 * Provide data encryption and decryption for Android P+ device.
 */
@RequiresApi(api = Build.VERSION_CODES.P)
class AndroidPEncryptor extends AndroidNEncryptor {

    private static final String TAG = AndroidPEncryptor.class.getSimpleName();

    AndroidPEncryptor(Context context, @NonNull String keyAlias) {
        super(keyAlias);
        //Allow access the data during screen lock
        specBuilder.setUnlockedDeviceRequired(false);

        if (context.getPackageManager().hasSystemFeature(FEATURE_STRONGBOX_KEYSTORE)) {
            Logger.debug(TAG, "Strong box keystore is used.");
            specBuilder.setIsStrongBoxBacked(true);
        }
    }

    @Override
    protected SecretKey getSecretKey() throws GeneralSecurityException, IOException {
        try {
            return super.getSecretKey();
        } catch (StrongBoxUnavailableException e) {
            //In case failed to use Strong Box, disable it.
            Logger.warn(TAG, "Strong Box unavailable, recover without strong box", e);
            specBuilder.setIsStrongBoxBacked(false);
            return super.getSecretKey();
        }
    }
}