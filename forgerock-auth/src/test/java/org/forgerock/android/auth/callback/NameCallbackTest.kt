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
class NameCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "NameCallback",
            "output": [
                {
                    "name": "prompt",
                    "value": "User Name"
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
        val nameCallback = NameCallback(raw, 0)
        Assert.assertEquals("User Name", nameCallback.getPrompt())
        nameCallback.setName("tester")
        Assert.assertEquals((nameCallback.contentAsJson.getJSONArray("input")[0] as JSONObject).getString(
            "value"),
            "tester")
        Assert.assertEquals(0, nameCallback.get_id().toLong())
    }
}