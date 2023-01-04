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
class ChoiceCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "ChoiceCallback",
            "output": [
                {
                    "name": "prompt",
                    "value": "Second Factor"
                },
                {
                    "name": "choices",
                    "value": [
                        "email",
                        "sms"
                    ]
                },
                {
                    "name": "defaultChoice",
                    "value": 0
                }
            ],
            "input": [
                {
                    "name": "IDToken1",
                    "value": 0
                }
            ]
        }""")
        val choiceCallback = ChoiceCallback(raw, 0)
        Assert.assertEquals("Second Factor", choiceCallback.getPrompt())
        choiceCallback.setSelectedIndex(1)
        Assert.assertEquals(1,
            (choiceCallback.contentAsJson.getJSONArray("input")[0] as JSONObject).getInt("value")
                .toLong())
    }
}