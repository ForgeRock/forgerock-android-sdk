/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.util;

import static org.junit.Assert.assertEquals;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class DeviceUtilsTest {

    private Context context;
    private ContentResolver contentResolver;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        contentResolver = context.getContentResolver();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.N_MR1)
    public void testGetDeviceNameFromSettingsSuccess() {
        // Given
        String expectedDeviceName = "Test Device Name";
        Settings.Global.putString(contentResolver, Settings.Global.DEVICE_NAME, expectedDeviceName);

        // When
        String actualDeviceName = DeviceUtils.getDeviceName(context);

        // Then
        assertEquals(expectedDeviceName, actualDeviceName);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void testGetDeviceNameFromBuildLowerThanN_MR1() {
        // Given
        String expectedDeviceName = getDeviceNameFromBuild();

        // When
        String actualDeviceName = DeviceUtils.getDeviceName(context);

        // Then
        assertEquals(expectedDeviceName, actualDeviceName);
    }

    @Test
    public void testGetDeviceNameFromBuildManufacturerAndModelSame() {
        // Given
        String manufacturer = "Google";
        String model = "google Pixel";
        String expectedDeviceName = "Google Pixel";
        mockBuild(manufacturer, model);

        // When
        String actualDeviceName = DeviceUtils.getDeviceName(context);

        // Then
        assertEquals(expectedDeviceName, actualDeviceName);
    }

    @Test
    public void testGetDeviceNameFromBuildManufacturerAndModelDifferent() {
        // Given
        String manufacturer = "Samsung";
        String model = "SM-G991B";
        String expectedDeviceName = "Samsung SM-G991B";
        mockBuild(manufacturer, model);

        // When
        String actualDeviceName = DeviceUtils.getDeviceName(context);

        // Then
        assertEquals(expectedDeviceName, actualDeviceName);
    }

    @Test
    public void testGetDeviceNameFromBuildException() {
        // Given
        mockBuild(null, null);

        // When
        String actualDeviceName = DeviceUtils.getDeviceName(context);

        // Then
        assertEquals("Unknown Android Device", actualDeviceName);
    }

    @Test
    public void testGetDeviceNameFromBuildEmptyManufacturer() {
        // Given
        String manufacturer = "";
        String model = "SM-G991B";
        String expectedDeviceName = "SM-G991B";
        mockBuild(manufacturer, model);

        // When
        String actualDeviceName = DeviceUtils.getDeviceName(context);

        // Then
        assertEquals(expectedDeviceName, actualDeviceName);
    }

    @Test
    public void testGetDeviceNameFromBuildEmptyModel() {
        // Given
        String manufacturer = "Samsung";
        String model = "";
        String expectedDeviceName = "Samsung";
        mockBuild(manufacturer, model);

        // When
        String actualDeviceName = DeviceUtils.getDeviceName(context).trim();

        // Then
        assertEquals(expectedDeviceName, actualDeviceName);
    }

    private void mockBuild(String manufacturer, String model) {
        try {
            java.lang.reflect.Field manufacturerField = Build.class.getDeclaredField("MANUFACTURER");
            manufacturerField.setAccessible(true);
            java.lang.reflect.Field modelField = Build.class.getDeclaredField("MODEL");
            modelField.setAccessible(true);
            manufacturerField.set(null, manufacturer);
            modelField.set(null, model);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getDeviceNameFromBuild() {
        try {
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
                return capitalize(model);
            } else {
                return capitalize(manufacturer) + " " + model;
            }
        } catch (Exception e) {
            return "Unknown Android Device";
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
