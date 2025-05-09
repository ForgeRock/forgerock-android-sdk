/*
 * Copyright (c) 2023 - 2025 Ping Identity Corporation. All rights reserved.
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
class SuspendedTextOutputCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "SuspendedTextOutputCallback",
            "output": [
                {
                    "name": "message",
                    "value": "This is a Message Node"
                },
                {
                    "name": "messageType",
                    "value": "1"
                }
            ]
        }""")
        val callback = SuspendedTextOutputCallback(raw, 0)
        Assert.assertEquals("This is a Message Node", callback.message)
        Assert.assertEquals(1, callback.messageType.toLong())
    }
}