/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.os.Build;
import androidx.annotation.RequiresApi;
import lombok.NonNull;

/**
 * Provide data encryption and decryption for Android N+ device.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class AndroidNEncryptor extends AndroidMEncryptor {

    AndroidNEncryptor(@NonNull String keyAlias, KeyUpdatedListener listener) {
        super(keyAlias, listener);
        specBuilder.setInvalidatedByBiometricEnrollment(true);
    }
}