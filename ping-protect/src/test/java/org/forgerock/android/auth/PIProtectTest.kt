/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth


import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingidentity.signalssdk.sdk.GetDataCallback
import com.pingidentity.signalssdk.sdk.InitCallback
import com.pingidentity.signalssdk.sdk.POInitParams
import com.pingidentity.signalssdk.sdk.PingOneSignals
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PIProtectTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        PIProtect.protectParamState = null
    }

    @Test
    fun testPauseBehaviourDataCalled() {
        mockkStatic(PingOneSignals::class)
        every { PingOneSignals.pauseBehavioralData() }.returns(Unit)
        PIProtect.pauseBehavioralData()
        verify { PingOneSignals.pauseBehavioralData() }
    }

    @Test
    fun testResumeBehaviourDataCalled() {
        mockkStatic(PingOneSignals::class)
        every { PingOneSignals.resumeBehavioralData() }.returns(Unit)
        PIProtect.resumeBehavioralData()
        verify { PingOneSignals.resumeBehavioralData() }
    }

    @Test
    fun testInitSDKDefaultValue() = runBlocking {
        mockkStatic(PingOneSignals::class)
        val mockSlot = slot<InitCallback>()
        val mockParamSlot = slot<POInitParams>()
        every { PingOneSignals.setInitCallback(capture(mockSlot)) }.answers {
            mockSlot.captured.onInitialized()
        }
        every { PingOneSignals.init(context, capture(mockParamSlot)) }.returns(Unit)
        assertNull(PIProtect.protectParamState)
        PIProtect.start(context)
        assertNotNull(PIProtect.protectParamState)
        assertEquals(null, mockParamSlot.captured.envId)
        assertEquals(false, mockParamSlot.captured.isConsoleLogEnabled)
        assertEquals(true, mockParamSlot.captured.isBehavioralDataCollection)
        assertEquals(false, mockParamSlot.captured.isLazyMetadata)
        assertEquals(null, mockParamSlot.captured.deviceAttributesToIgnore)
        assertEquals(null, mockParamSlot.captured.customHost)
        verify(exactly = 1) { PingOneSignals.setInitCallback(any()) }
        PIProtect.start(context)
        verify(exactly = 1) { PingOneSignals.setInitCallback(any()) }
    }

    @Test
    fun testInitSDKWithSomeValue() = runBlocking {
        mockkStatic(PingOneSignals::class)
        val mockSlot = slot<InitCallback>()
        val mockParamSlot = slot<POInitParams>()
        every { PingOneSignals.setInitCallback(capture(mockSlot)) }.answers {
            mockSlot.captured.onInitialized()
        }
        every { PingOneSignals.init(context, capture(mockParamSlot)) }.returns(Unit)
        assertNull(PIProtect.protectParamState)
        val init = PIInitParams(envId = "jey", isConsoleLogEnabled = true, deviceAttributesToIgnore = listOf("value"))
        PIProtect.start(context, init)
        assertNotNull(PIProtect.protectParamState)
        assertEquals("jey", mockParamSlot.captured.envId)
        assertEquals(true, mockParamSlot.captured.isConsoleLogEnabled)
        assertEquals(true, mockParamSlot.captured.isBehavioralDataCollection)
        assertEquals(false, mockParamSlot.captured.isLazyMetadata)
        assertEquals(listOf("value"), mockParamSlot.captured.deviceAttributesToIgnore)
        assertEquals(null, mockParamSlot.captured.customHost)
        verify(exactly = 1) { PingOneSignals.setInitCallback(any()) }
        PIProtect.start(context)
        verify(exactly = 1) { PingOneSignals.setInitCallback(any()) }
    }

    @Test
    fun testErrorInit() = runBlocking {
        mockkStatic(PingOneSignals::class)
        val mockSlot = slot<InitCallback>()
        val mockParamSlot = slot<POInitParams>()
        every { PingOneSignals.setInitCallback(capture(mockSlot)) }.answers {
            mockSlot.captured.onError("error", "init" , "sdk")
        }
        every { PingOneSignals.init(context, capture(mockParamSlot)) }.returns(Unit)
        assertNull(PIProtect.protectParamState)
        try{
            PIProtect.start(context)
            fail()
        }
        catch (e: Exception) {
            assertNull(PIProtect.protectParamState)
            assertTrue(e is PingOneProtectInitException)
        }
    }

    @Test
    fun testGetData() = runBlocking {
        mockkStatic(PingOneSignals::class)
        every { PingOneSignals.pauseBehavioralData() }.returns(Unit)
        val mockSlot = slot<GetDataCallback>()
        every { PingOneSignals.getData(capture(mockSlot)) }.answers {
            mockSlot.captured.onSuccess("test")
        }
        val value = PIProtect.getData()
        assertEquals("test", value)
    }

    @Test
    fun testGetFailure() = runBlocking {
        mockkStatic(PingOneSignals::class)
        every { PingOneSignals.pauseBehavioralData() }.returns(Unit)
        val mockSlot = slot<GetDataCallback>()
        every { PingOneSignals.getData(capture(mockSlot)) }.answers {
            mockSlot.captured.onFailure("test")
        }
        try {
            PIProtect.getData()
            fail()
        } catch (e: Exception) {
            assertTrue(e is PingOneProtectEvaluationException)
        }
        verify(exactly = 0) { PingOneSignals.pauseBehavioralData() }
    }

}