/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.authenticator.sample.controller;

import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.forgerock.android.auth.Logger;

/**
 * Utility class used to check Google Play Services availability
 */
public class GooglePlayServicesUtil {

    private static final String TAG = GooglePlayServicesUtil.class.getSimpleName();

    /**
     * Check if Google Play Services is installed and enabled on this device
     *
     * @param context the context
     * @return True if Google Play Services is available, false otherwise.
     * @throws IllegalStateException If an error occur when checking Google Play Services availability
     */
    public static boolean isAvailable(Context context) throws IllegalStateException {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                // Uncomment the line bellow if you want to show the error
                //apiAvailability.showErrorNotification(context, resultCode);
                Logger.warn(TAG, "Google Play Services not available.");
            } else {
                Logger.warn(TAG, "Error trying to check Google Play Services availability.");
            }
            return false;
        }
        return true;
    }

}
