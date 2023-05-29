/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PollingWaitCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "PollingWaitCallback",
            "output": [
                {
                    "name": "waitTime",
                    "value": "30000"
                },
                {
                    "name": "message",
                    "value": "Please Wait. Processing ..."
                }
            ]
        }""")
        val pollingWaitCallback = PollingWaitCallback(raw, 0)
        Assert.assertEquals("Please Wait. Processing ...", pollingWaitCallback.message)
        Assert.assertEquals("30000", pollingWaitCallback.waitTime)
    }
}