/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.forgerock.android.auth.FRListenerFuture
import org.robolectric.shadows.ShadowApplication
import org.robolectric.Shadows
import kotlin.Throws
import org.json.JSONException
import org.forgerock.android.auth.collector.DeviceCollector
import org.forgerock.android.auth.collector.BluetoothCollector
import org.forgerock.android.auth.collector.BrowserCollector
import org.forgerock.android.auth.collector.CameraCollector
import org.forgerock.android.auth.collector.NetworkCollector
import org.forgerock.android.auth.collector.HardwareCollector
import org.forgerock.android.auth.collector.PlatformCollector
import org.forgerock.android.auth.collector.TelephonyCollector
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ExecutionException

@RunWith(AndroidJUnit4::class)
class FRDeviceTest {

    var context = ApplicationProvider.getApplicationContext<Context>()
    private var listener: FRListenerFuture<JSONObject>? = null
    @Before
    fun setUp() {
        Config.getInstance().init(context, null)
        val application = ApplicationProvider.getApplicationContext<Application>()
        val app = Shadows.shadowOf(application)
        app.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH)
        listener = FRListenerFuture()
    }

    @Test
    @Throws(JSONException::class, ExecutionException::class, InterruptedException::class)
    fun testBluetoothCollector() {
        val collector: DeviceCollector = BluetoothCollector()
        collector.collect(context, listener)
        Assert.assertTrue(listener!!.get().getBoolean("supported"))
    }

    @Test
    @Throws(JSONException::class, ExecutionException::class, InterruptedException::class)
    fun testBrowserCollector() {
        System.setProperty("http.agent", "test")
        val collector: DeviceCollector = BrowserCollector()
        collector.collect(context, listener)
        Assert.assertEquals("user", listener!!.get().getString("userAgent"))
    }

    @Test
    @Throws(JSONException::class, ExecutionException::class, InterruptedException::class)
    fun testCameraCollector() {
        val collector: DeviceCollector = CameraCollector()
        collector.collect(context, listener)
        Assert.assertEquals(0, listener!!.get().getInt("numberOfCameras").toLong())
    }

    @Test
    @Throws(JSONException::class, ExecutionException::class, InterruptedException::class)
    fun testNetworkCollector() {
        val collector: DeviceCollector = NetworkCollector()
        collector.collect(context, listener)
        Assert.assertTrue(listener!!.get().getBoolean("connected"))
    }

    @Test
    @Throws(JSONException::class, ExecutionException::class, InterruptedException::class)
    fun testHardwareCollector() {
        val collector: DeviceCollector = HardwareCollector()
        collector.collect(context, listener)
        val result = listener!!.get()
        Assert.assertNotNull(result.getString("cpu"))
        Assert.assertEquals("robolectric", result.getString("hardware"))
        Assert.assertEquals("unknown", result.getString("manufacturer"))
        Assert.assertEquals(0, result.getInt("storage").toLong())
        Assert.assertEquals(0, result.getInt("memory").toLong())
        //assertNotNull(result.getJSONObject("display").getString("displayId"));
        Assert.assertEquals(0, result.getJSONObject("camera").getInt("numberOfCameras").toLong())
    }

    @Test
    @Throws(JSONException::class, ExecutionException::class, InterruptedException::class)
    fun testPlatformCollector() {
        val collector: DeviceCollector = PlatformCollector()
        collector.collect(context, listener)
        val result = listener!!.get()
        Assert.assertEquals("Android", result.getString("platform"))
        Assert.assertEquals("robolectric", result.getString("device"))
        Assert.assertEquals("robolectric", result.getString("model"))
        Assert.assertEquals("Android", result.getString("brand"))
        Assert.assertEquals("en_US", result.getString("locale"))
        Assert.assertNotNull(result.getString("timeZone"))
        Assert.assertEquals(1.0, result.getDouble("jailBreakScore"), 0.0001)
    }

    @Test
    @Throws(JSONException::class, ExecutionException::class, InterruptedException::class)
    fun testTelephonyCollector() {
        val collector: DeviceCollector = TelephonyCollector()
        collector.collect(context, listener)
        val result = listener!!.get()
        Assert.assertEquals("", result.getString("carrierName"))
        //assertEquals("", result.getString("networkOperator"));
        //assertEquals("UNKNOWN", result.getString("networkType"));
        //assertEquals("GSM", result.getString("phoneType"));
        //assertEquals("", result.getString("simIsoCountryCode"));
        //assertFalse(result.getBoolean("isRoamingNetwork"));
    }
}