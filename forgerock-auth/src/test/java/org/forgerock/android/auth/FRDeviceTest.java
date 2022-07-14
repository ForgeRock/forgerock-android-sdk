/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.Manifest;
import android.app.Application;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.forgerock.android.auth.collector.BluetoothCollector;
import org.forgerock.android.auth.collector.BrowserCollector;
import org.forgerock.android.auth.collector.CameraCollector;
import org.forgerock.android.auth.collector.DeviceCollector;
import org.forgerock.android.auth.collector.HardwareCollector;
import org.forgerock.android.auth.collector.NetworkCollector;
import org.forgerock.android.auth.collector.PlatformCollector;
import org.forgerock.android.auth.collector.TelephonyCollector;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class FRDeviceTest {
    public Context context = ApplicationProvider.getApplicationContext();
    private FRListenerFuture<JSONObject> listener;

    @Before
    public void setUp() {
        Config.getInstance().init(context,null);
        Application application = ApplicationProvider.getApplicationContext();
        ShadowApplication app = Shadows.shadowOf(application);
        app.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH);
        listener = new FRListenerFuture<>();

    }

    @Test
    public void testBluetoothCollector() throws JSONException, ExecutionException, InterruptedException {
        DeviceCollector collector = new BluetoothCollector();
        collector.collect(context, listener);
        assertTrue(listener.get().getBoolean("supported"));
    }

    @Test
    public void testBrowserCollector() throws JSONException, ExecutionException, InterruptedException {
        System.setProperty("http.agent", "test");
        DeviceCollector collector = new BrowserCollector();
        collector.collect(context, listener);
        Assert.assertEquals("user", listener.get().getString("userAgent"));
    }

    @Test
    public void testCameraCollector() throws JSONException, ExecutionException, InterruptedException {
        DeviceCollector collector = new CameraCollector();
        collector.collect(context, listener);
        Assert.assertEquals(0, listener.get().getInt("numberOfCameras"));
    }

    @Test
    public void testNetworkCollector() throws JSONException, ExecutionException, InterruptedException {
        DeviceCollector collector = new NetworkCollector();
        collector.collect(context, listener);
        assertTrue(listener.get().getBoolean("connected"));
    }

    @Test
    public void testHardwareCollector() throws JSONException, ExecutionException, InterruptedException {
        DeviceCollector collector = new HardwareCollector();
        collector.collect(context, listener);
        JSONObject result = listener.get();
        assertNotNull(result.getString("cpu"));
        assertEquals("robolectric", result.getString("hardware"));
        assertEquals("unknown", result.getString("manufacturer"));
        assertEquals(0, result.getInt("storage"));
        assertEquals(0, result.getInt("memory"));
        //assertNotNull(result.getJSONObject("display").getString("displayId"));
        assertEquals(0, result.getJSONObject("camera").getInt("numberOfCameras"));
    }

    @Test
    public void testPlatformCollector() throws JSONException, ExecutionException, InterruptedException {
        DeviceCollector collector = new PlatformCollector();
        collector.collect(context, listener);
        JSONObject result = listener.get();

        assertEquals("Android", result.getString("platform"));
        assertEquals("robolectric", result.getString("device"));
        assertEquals("robolectric", result.getString("model"));
        assertEquals("Android", result.getString("brand"));
        assertEquals("en_US", result.getString("locale"));
        assertNotNull(result.getString("timeZone"));
        assertEquals(1.0, result.getDouble("jailBreakScore"), 0.0001);
    }

    @Test
    public void testTelephonyCollector() throws JSONException, ExecutionException, InterruptedException {
        DeviceCollector collector = new TelephonyCollector();
        collector.collect(context, listener);
        JSONObject result = listener.get();

        assertEquals("", result.getString("carrierName"));
        //assertEquals("", result.getString("networkOperator"));
        //assertEquals("UNKNOWN", result.getString("networkType"));
        //assertEquals("GSM", result.getString("phoneType"));
        //assertEquals("", result.getString("simIsoCountryCode"));
        //assertFalse(result.getBoolean("isRoamingNetwork"));
    }
}
