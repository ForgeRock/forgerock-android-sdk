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
class ConfirmationCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "ConfirmationCallback",
            "output": [
                {
                    "name": "prompt",
                    "value": ""
                },
                {
                    "name": "messageType",
                    "value": 0
                },
                {
                    "name": "options",
                    "value": [
                        "I can't wait, please let me go."
                    ]
                },
                {
                    "name": "optionType",
                    "value": -1
                },
                {
                    "name": "defaultOption",
                    "value": 0
                }
            ],
            "input": [
                {
                    "name": "IDToken2",
                    "value": 100
                }
            ]
        }""")
        val confirmationCallback = ConfirmationCallback(raw, 0)
        Assert.assertEquals("", confirmationCallback.getPrompt())
        Assert.assertEquals(ConfirmationCallback.INFORMATION.toLong(),
            confirmationCallback.messageType.toLong())
        Assert.assertEquals("I can't wait, please let me go.", confirmationCallback.options[0])
        Assert.assertEquals(ConfirmationCallback.UNSPECIFIED_OPTION.toLong(),
            confirmationCallback.optionType.toLong())
        Assert.assertEquals(ConfirmationCallback.YES.toLong(),
            confirmationCallback.defaultOption.toLong())
        confirmationCallback.selectedIndex = 0
        Assert.assertEquals(0,
            (confirmationCallback.contentAsJson.getJSONArray("input")[0] as JSONObject).getInt("value")
                .toLong())
    }
}