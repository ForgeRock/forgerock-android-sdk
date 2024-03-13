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
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PingOneProtectInitializeCallbackTest {

    val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }
    @Test
    fun basicTest() {
        val json = "{\"type\":\"PingOneProtectInitializeCallback\",\"output\":[{\"name\":\"envId\",\"value\":\"02fb4743-189a-4bc7-9d6c-a919edfe6447\"},{\"name\":\"consoleLogEnabled\",\"value\":true},{\"name\":\"deviceAttributesToIgnore\",\"value\":[]},{\"name\":\"customHost\",\"value\":\"\"},{\"name\":\"lazyMetadata\",\"value\":false},{\"name\":\"behavioralDataCollection\",\"value\":true},{\"name\":\"deviceKeyRsyncIntervals\",\"value\":14},{\"name\":\"enableTrust\",\"value\":false},{\"name\":\"disableTags\",\"value\":false},{\"name\":\"disableHub\",\"value\":false}],\"input\":[{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val raw = JSONObject(json)
        val pingOneInitCallback = PingOneProtectInitializeCallback(raw, 0)
        assertEquals("02fb4743-189a-4bc7-9d6c-a919edfe6447",
            pingOneInitCallback.envId)
        assertEquals(true,
            pingOneInitCallback.behavioralDataCollection)
        assertEquals(true,
            pingOneInitCallback.consoleLogEnabled)
    }

    @Test
    fun basicTestDifferentParam() {
        val json = "{\"type\":\"PingOneProtectInitializeCallback\",\"output\":[{\"name\":\"envId\",\"value\":\"02fb4743-189a-4bc7-9d6c-a919edfe6447\"},{\"name\":\"consoleLogEnabled\",\"value\":false},{\"name\":\"deviceAttributesToIgnore\",\"value\":[]},{\"name\":\"customHost\",\"value\":\"\"},{\"name\":\"lazyMetadata\",\"value\":false},{\"name\":\"behavioralDataCollection\",\"value\":false},{\"name\":\"deviceKeyRsyncIntervals\",\"value\":14},{\"name\":\"enableTrust\",\"value\":false},{\"name\":\"disableTags\",\"value\":false},{\"name\":\"disableHub\",\"value\":false}],\"input\":[{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val raw = JSONObject(json)
        val pingOneInitCallback = PingOneProtectInitializeCallback(raw, 0)
        assertEquals("02fb4743-189a-4bc7-9d6c-a919edfe6447",
            pingOneInitCallback.envId)
        assertEquals(false,
            pingOneInitCallback.behavioralDataCollection)
        assertEquals(false,
            pingOneInitCallback.consoleLogEnabled)
    }

    @Test
    fun  initMethodCalled() = runBlocking {
        mockkObject(PIProtect)
        val mockSlot = slot<PIInitParams>()
        coEvery {
            PIProtect.start(context, capture(mockSlot))
        } returns(Unit)
        coEvery {
            PIProtect.resumeBehavioralData()
        } returns(Unit)
        coEvery {
            PIProtect.pauseBehavioralData()
        } returns(Unit)
        try {
            val json =
                "{\"type\":\"PingOneProtectInitializeCallback\",\"output\":[{\"name\":\"envId\",\"value\":\"02fb4743-189a-4bc7-9d6c-a919edfe6447\"},{\"name\":\"consoleLogEnabled\",\"value\":false},{\"name\":\"deviceAttributesToIgnore\",\"value\":[\"value1\", \"value2\"]},{\"name\":\"customHost\",\"value\":\"\"},{\"name\":\"lazyMetadata\",\"value\":false},{\"name\":\"behavioralDataCollection\",\"value\":true},{\"name\":\"deviceKeyRsyncIntervals\",\"value\":14},{\"name\":\"enableTrust\",\"value\":false},{\"name\":\"disableTags\",\"value\":false},{\"name\":\"disableHub\",\"value\":false}],\"input\":[{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
            val raw = JSONObject(json)
            val pingOneInitCallback = PingOneProtectInitializeCallback(raw, 0)
            pingOneInitCallback.start(context)
            coVerify { PIProtect.start(context, any()) }
            assertEquals(mockSlot.captured.envId, "02fb4743-189a-4bc7-9d6c-a919edfe6447")
            assertEquals(mockSlot.captured.isBehavioralDataCollection, true)
            assertEquals(mockSlot.captured.isConsoleLogEnabled, false)
            assertEquals(mockSlot.captured.isLazyMetadata, false)
            assertEquals(mockSlot.captured.customHost, "")
            assertEquals(mockSlot.captured.deviceAttributesToIgnore, listOf("value1", "value2"))
            coVerify(exactly = 1) { PIProtect.resumeBehavioralData() }
            coVerify(exactly = 0) { PIProtect.pauseBehavioralData() }
        } catch (e: Exception) {
            fail()
        }
    }

    @Test
    fun  initMethodCalledPause() = runBlocking {
        mockkObject(PIProtect)
        val mockSlot = slot<PIInitParams>()
        coEvery {
            PIProtect.start(context, capture(mockSlot))
        } returns(Unit)
        coEvery {
            PIProtect.resumeBehavioralData()
        } returns(Unit)
        coEvery {
            PIProtect.pauseBehavioralData()
        } returns(Unit)
        try {
            val json =
                "{\"type\":\"PingOneProtectInitializeCallback\",\"output\":[{\"name\":\"envId\",\"value\":\"02fb4743-189a-4bc7-9d6c-a919edfe6447\"},{\"name\":\"consoleLogEnabled\",\"value\":false},{\"name\":\"deviceAttributesToIgnore\",\"value\":[\"value1\", \"value2\"]},{\"name\":\"customHost\",\"value\":\"\"},{\"name\":\"lazyMetadata\",\"value\":false},{\"name\":\"behavioralDataCollection\",\"value\":false},{\"name\":\"deviceKeyRsyncIntervals\",\"value\":14},{\"name\":\"enableTrust\",\"value\":false},{\"name\":\"disableTags\",\"value\":false},{\"name\":\"disableHub\",\"value\":false}],\"input\":[{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
            val raw = JSONObject(json)
            val pingOneInitCallback = PingOneProtectInitializeCallback(raw, 0)
            pingOneInitCallback.start(context)
            coVerify { PIProtect.start(context, any()) }
            assertEquals(mockSlot.captured.envId, "02fb4743-189a-4bc7-9d6c-a919edfe6447")
            assertEquals(mockSlot.captured.isBehavioralDataCollection, false)
            assertEquals(mockSlot.captured.isConsoleLogEnabled, false)
            assertEquals(mockSlot.captured.isLazyMetadata, false)
            assertEquals(mockSlot.captured.customHost, "")
            assertEquals(mockSlot.captured.deviceAttributesToIgnore, listOf("value1", "value2"))
            coVerify(exactly = 0) { PIProtect.resumeBehavioralData() }
            coVerify(exactly = 1) { PIProtect.pauseBehavioralData() }
        } catch (e: Exception) {
            fail()
        }
    }

    @Test
    fun `test exception`() = runBlocking {
        mockkObject(PIProtect)
        val mockSlot = slot<PIInitParams>()
        coEvery {
            PIProtect.start(context, capture(mockSlot))
        } throws(PingOneProtectInitException("init failed"))
        coEvery {
            PIProtect.resumeBehavioralData()
        } returns(Unit)
        coEvery {
            PIProtect.pauseBehavioralData()
        } returns(Unit)
        try {
            val json =
                "{\"type\":\"PingOneProtectInitializeCallback\",\"output\":[{\"name\":\"envId\",\"value\":\"02fb4743-189a-4bc7-9d6c-a919edfe6447\"},{\"name\":\"consoleLogEnabled\",\"value\":false},{\"name\":\"deviceAttributesToIgnore\",\"value\":[\"value1\", \"value2\"]},{\"name\":\"customHost\",\"value\":\"\"},{\"name\":\"lazyMetadata\",\"value\":false},{\"name\":\"behavioralDataCollection\",\"value\":false},{\"name\":\"deviceKeyRsyncIntervals\",\"value\":14},{\"name\":\"enableTrust\",\"value\":false},{\"name\":\"disableTags\",\"value\":false},{\"name\":\"disableHub\",\"value\":false}],\"input\":[{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
            val raw = JSONObject(json)
            val pingOneInitCallback = PingOneProtectInitializeCallback(raw, 0)
            pingOneInitCallback.start(context)
            fail()
        } catch (e: Exception) {
            coVerify(exactly = 1) { PIProtect.start(context, any()) }
            coVerify(exactly = 0) { PIProtect.resumeBehavioralData() }
            coVerify(exactly = 0) { PIProtect.pauseBehavioralData() }
            assertTrue(e is PingOneProtectInitException)
        }
    }
}