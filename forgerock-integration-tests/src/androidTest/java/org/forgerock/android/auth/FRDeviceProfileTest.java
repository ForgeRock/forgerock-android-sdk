/*
 * Copyright (c) 2019 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.GrantPermissionRule;

import org.forgerock.android.auth.collector.BluetoothCollector;
import org.forgerock.android.auth.collector.DeviceCollector;
import org.forgerock.android.auth.collector.FRDeviceCollector;
import org.forgerock.android.auth.collector.TelephonyCollector;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class FRDeviceProfileTest extends AndroidBaseTest {
    private static final String TAG = FRDeviceProfileTest.class.getSimpleName();

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.BLUETOOTH
    );

    @Rule
    public SkipTestOnPermissionFailureRule skipRule = new SkipTestOnPermissionFailureRule();

    @Before
    public void setUp() throws Exception {
        Config.getInstance().init(context, null);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void testDeviceProfile() throws JSONException, ExecutionException, InterruptedException {
        Logger.set(Logger.Level.DEBUG);

        FRListenerFuture<JSONObject> future = new FRListenerFuture<>();
        FRDevice.getInstance().getProfile(future);
        JSONObject result = future.get();
        assertNotNull(result.getString("identifier"));
        assertNotNull(result.getString("version"));

        assertNotNull(result.getJSONObject("platform").getString("platform"));
        assertTrue(result.getJSONObject("platform").optInt("version", -1) != -1);
        assertNotNull(result.getJSONObject("platform").getString("device"));
        assertNotNull(result.getJSONObject("platform").getString("deviceName"));
        assertNotNull(result.getJSONObject("platform").getString("model"));
        assertNotNull(result.getJSONObject("platform").getString("brand"));
        assertNotNull(result.getJSONObject("platform").getString("locale"));
        assertNotNull(result.getJSONObject("platform").getString("timeZone"));
        result.getJSONObject("platform").getDouble("jailBreakScore");

        assertTrue(result.getJSONObject("hardware").optInt("cpu", -1) != -1);
        assertNotNull(result.getJSONObject("hardware").getString("hardware"));
        assertNotNull(result.getJSONObject("hardware").getString("manufacturer"));
        assertTrue(result.getJSONObject("hardware").optInt("storage", -1) != -1);
        assertTrue(result.getJSONObject("hardware").optInt("memory", -1) != -1);
        //assertNotNull(result.getJSONObject("hardware").getJSONObject("display").getString("displayId"));
        assertTrue(result.getJSONObject("hardware").getJSONObject("display").optInt("width", -1) != -1);
        assertTrue(result.getJSONObject("hardware").getJSONObject("display").optInt("height", -1) != -1);
        assertTrue(result.getJSONObject("hardware").getJSONObject("display").optInt("orientation", -1) != -1);
        assertTrue(result.getJSONObject("hardware").getJSONObject("camera").optInt("numberOfCameras", -1) != -1);

        assertNotNull(result.getJSONObject("browser").getString("userAgent"));

        result.getJSONObject("network").getBoolean("connected");

        //assertNotNull(result.getJSONObject("connectivity").getJSONObject("network").getString("macAddress"));
        result.getJSONObject("bluetooth").getBoolean("supported");

        assertNotNull(result.getJSONObject("telephony").getString("networkCountryIso"));
        assertNotNull(result.getJSONObject("telephony").getString("carrierName"));
        /*
        assertNotNull(result.getJSONObject("telephony").getString("networkOperator"));
        assertNotNull(result.getJSONObject("telephony").getString("networkType"));
        assertNotNull(result.getJSONObject("telephony").getString("phoneType"));
        assertNotNull(result.getJSONObject("telephony").getString("simIsoCountryCode"));
        assertFalse(result.getJSONObject("telephony").getBoolean("isRoamingNetwork"));
        */

        // Location may not be captured using emulator
        // or if ACCESS_BACKGROUND_LOCATION permission is not granted
        boolean locationPermissionGranted = false;

        // If the location services are NOT enabled skip the rest of the test...
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Logger.debug(TAG, "Location service is disabled. Skipping the rest of the test...");
            return;
        }

        // If there are no providers enabled skip the rest of the test. Providers can be "network", "gps" or "passive"...
        List<String> providers = locationManager.getProviders(true);
        if(providers.isEmpty()) {
            Logger.debug(TAG, "No location providers are available. Skipping the rest of the test...");
            return;
        }

        // If none of the location permissions are granted then location info is omitted...
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            Logger.debug(TAG, "Location permissions are granted!...");
            locationPermissionGranted = true;
        }

        if (!isEmulator() && locationPermissionGranted) {
            Logger.debug(TAG, "Location data should exist!");
            Logger.debug(TAG, result.toString());
            assertTrue(result.getJSONObject("location").has("latitude"));
            assertTrue(result.getJSONObject("location").has("longitude"));
        }

    }

    @Test
    public void customCollector() throws JSONException, ExecutionException, InterruptedException {
        FRDeviceCollector collector = FRDeviceCollector.builder().collector(new DeviceCollector() {
            @Override
            public String getName() {
                return "test";
            }

            @Override
            public void collect(Context context, FRListener<JSONObject> listener) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("name", "value");
                    listener.onSuccess(jsonObject);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }).build();

        FRListenerFuture<JSONObject> result = new FRListenerFuture<>();
        collector.collect(context, result);
        assertEquals("value", result.get().getJSONObject("test").getString("name"));

    }

    @Test
    public void multipleCustomCollector() throws JSONException, ExecutionException, InterruptedException {
        FRDeviceCollector collector = FRDeviceCollector.builder()
                .collector(new BluetoothCollector())
                .collector(new TelephonyCollector())
                .build();

        FRListenerFuture<JSONObject> result = new FRListenerFuture<>();
        collector.collect(context, result);
        result.get().getJSONObject("bluetooth").getBoolean("supported");
        result.get().getJSONObject("telephony").getString("networkCountryIso");
    }

    private boolean isEmulator() {
        return Build.PRODUCT.matches(".*_?sdk_?.*");
    }
}
