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
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PingOneProtectEvaluationCallbackTest {

    val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }
    @Test
    fun basicTest() {
        val json = "{\"type\":\"PingOneProtectEvaluationCallback\",\"output\":[{\"name\":\"pauseBehavioralData\",\"value\":true}],\"input\":[{\"name\":\"IDToken1signals\",\"value\":\"HcSd8yBF7G0g0d0lzzz6rci4I0IB+VZdFl6bBiRN2RjMhjSsipf0h0JC4ganxwiBRzSoGwMMZagmq7Teoabm6cZ8X0mStPL\\/bzCCAQPc5wmGmW2M7GKEITiEQbgHN+ZB\\/cd8g6MmCJsYK5OYplbG\\/SDuCtWZDIS4mUdxywlTFDMmXm9tC2Fy5vfgk+9DX4eOSPIHiQq5wPpGsILrY17H87A4Qt4gb6ITC2s9Oo7qf8R0gfiJttuPyWYFL7w1KoiuUi6JPf5v2H0HW04Mc1qlZwD44Dd7RlHGQeGs\\/fk21KZ6kKI4cTd8eHAt3Vrl29yIhn6Ce\\/go1Ve\\/0qj0DWx703SRbuc5IBm8AR\\/q9DpxQkEd8PC8+FWBisuGLTQyjqTS6DCEy7LwgR0LU28Hwdw1jDeZgoZy54kCpo6v9B6x1\\/bkNH8YtlSt9uz\\/A9UinS4g0VdN09H6SXKXNxn4bYhJeWK7c4q9Byvuye1M08qh7JWzMKpkWyZwaeC6zIaMQhiwrodyjS+S25dBk1YcQ==.eDE=\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val raw = JSONObject(json)
        val pingOneEvalCallback = PingOneProtectEvaluationCallback(raw, 0)
        assertEquals(true,
            pingOneEvalCallback.pauseBehavioralData)
    }
    @Test
    fun initBehaviorData() = runBlocking {
        mockkObject(PIProtect)
        coEvery {
            PIProtect.getData()
        } returns("result")
        val json = "{\"type\":\"PingOneProtectEvaluationCallback\",\"output\":[{\"name\":\"pauseBehavioralData\",\"value\":true}],\"input\":[{\"name\":\"IDToken1signals\",\"value\":\"HcSd8yBF7G0g0d0lzzz6rci4I0IB+VZdFl6bBiRN2RjMhjSsipf0h0JC4ganxwiBRzSoGwMMZagmq7Teoabm6cZ8X0mStPL\\/bzCCAQPc5wmGmW2M7GKEITiEQbgHN+ZB\\/cd8g6MmCJsYK5OYplbG\\/SDuCtWZDIS4mUdxywlTFDMmXm9tC2Fy5vfgk+9DX4eOSPIHiQq5wPpGsILrY17H87A4Qt4gb6ITC2s9Oo7qf8R0gfiJttuPyWYFL7w1KoiuUi6JPf5v2H0HW04Mc1qlZwD44Dd7RlHGQeGs\\/fk21KZ6kKI4cTd8eHAt3Vrl29yIhn6Ce\\/go1Ve\\/0qj0DWx703SRbuc5IBm8AR\\/q9DpxQkEd8PC8+FWBisuGLTQyjqTS6DCEy7LwgR0LU28Hwdw1jDeZgoZy54kCpo6v9B6x1\\/bkNH8YtlSt9uz\\/A9UinS4g0VdN09H6SXKXNxn4bYhJeWK7c4q9Byvuye1M08qh7JWzMKpkWyZwaeC6zIaMQhiwrodyjS+S25dBk1YcQ==.eDE=\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val raw = JSONObject(json)
        val pingOneEvalCallback = PingOneProtectEvaluationCallback(raw, 0)
        pingOneEvalCallback.getData(context)
        assertTrue(pingOneEvalCallback.content.contains("result"))
        coVerify(exactly = 1) { PIProtect.getData() }
        coVerify(exactly = 1) { PIProtect.pauseBehavioralData() }
    }

    @Test
    fun `test exception`() = runBlocking {
        mockkObject(PIProtect)
        coEvery {
            PIProtect.getData()
        } throws(PingOneProtectEvaluationException("evaluation failed"))

        try {
            val json = "{\"type\":\"PingOneProtectEvaluationCallback\",\"output\":[{\"name\":\"pauseBehavioralData\",\"value\":true}],\"input\":[{\"name\":\"IDToken1signals\",\"value\":\"HcSd8yBF7G0g0d0lzzz6rci4I0IB+VZdFl6bBiRN2RjMhjSsipf0h0JC4ganxwiBRzSoGwMMZagmq7Teoabm6cZ8X0mStPL\\/bzCCAQPc5wmGmW2M7GKEITiEQbgHN+ZB\\/cd8g6MmCJsYK5OYplbG\\/SDuCtWZDIS4mUdxywlTFDMmXm9tC2Fy5vfgk+9DX4eOSPIHiQq5wPpGsILrY17H87A4Qt4gb6ITC2s9Oo7qf8R0gfiJttuPyWYFL7w1KoiuUi6JPf5v2H0HW04Mc1qlZwD44Dd7RlHGQeGs\\/fk21KZ6kKI4cTd8eHAt3Vrl29yIhn6Ce\\/go1Ve\\/0qj0DWx703SRbuc5IBm8AR\\/q9DpxQkEd8PC8+FWBisuGLTQyjqTS6DCEy7LwgR0LU28Hwdw1jDeZgoZy54kCpo6v9B6x1\\/bkNH8YtlSt9uz\\/A9UinS4g0VdN09H6SXKXNxn4bYhJeWK7c4q9Byvuye1M08qh7JWzMKpkWyZwaeC6zIaMQhiwrodyjS+S25dBk1YcQ==.eDE=\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
            val raw = JSONObject(json)
            val pingOneEvalCallback = PingOneProtectEvaluationCallback(raw, 0)
            pingOneEvalCallback.getData(context)
            fail()
        } catch (e: Exception) {
            coVerify(exactly = 1) { PIProtect.getData() }
            assertTrue(e is PingOneProtectEvaluationException)
        }
    }

}