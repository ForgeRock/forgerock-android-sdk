/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.util;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.Locale;

/**
 * Utility class for retrieving device information, including the device name.
 */
public class DeviceUtils {

    private static final String TAG = DeviceUtils.class.getSimpleName();

    private DeviceUtils() {
        // Private constructor to prevent instantiation.
        throw new AssertionError("No instances.");
    }

    /**
     * Gets the user-friendly name of the device.
     *
     * @param context The application context.
     * @return The device name, or a default value if the name cannot be determined.
     */
    public static String getDeviceName(Context context) {
        String deviceName = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            deviceName = getDeviceNameFromSettings(context);
        }

        if (deviceName != null) {
            return deviceName;
        }

        deviceName = getDeviceNameFromBuild();
        if (deviceName != null) {
            return deviceName;
        }

        return "Unknown Android Device";
    }

    /**
     * Gets the device name from the system settings.
     *
     * @param context The application context.
     * @return The device name from settings, or null if not found.
     */
    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private static String getDeviceNameFromSettings(Context context) {
        try {
            return Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME);
        } catch (Exception e) {
            Log.e(TAG, "Error getting device name from settings: ", e);
            return null;
        }
    }

    /**
     * Gets the device name from the Build class.
     *
     * @return The device name from Build, or null if not found.
     */
    private static String getDeviceNameFromBuild() {
        try {
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            if (model.toLowerCase(Locale.ROOT).startsWith(manufacturer.toLowerCase(Locale.ROOT))) {
                return capitalize(model);
            } else {
                return capitalize(manufacturer) + " " + model;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting device name from Build: ", e);
            return null;
        }
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str The string to capitalize.
     * @return The capitalized string.
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }
}
