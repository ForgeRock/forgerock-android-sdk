/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
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
class TextInputCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "TextInputCallback",
            "output": [
                {
                    "name": "prompt",
                    "value": "One Time Pin"
                },
                {
                    "name": "defaultText",
                    "value": ""
                }
            ],
            "input": [
                {
                    "name": "IDToken1",
                    "value": ""
                }
            ],
            "_id": 0
        }""")
        val textInputCallback = TextInputCallback(raw, 0)
        Assert.assertEquals("One Time Pin", textInputCallback.getPrompt())
        Assert.assertEquals("", textInputCallback.defaultText)
        textInputCallback.setValue("010101")
        Assert.assertEquals((textInputCallback.contentAsJson.getJSONArray("input")[0] as JSONObject).getString(
            "value"),
            "010101")
        Assert.assertEquals(0, textInputCallback.get_id().toLong())
    }
}