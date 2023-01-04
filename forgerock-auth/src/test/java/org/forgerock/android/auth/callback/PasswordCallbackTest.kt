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
class PasswordCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "PasswordCallback",
            "output": [
                {
                    "name": "prompt",
                    "value": "Password"
                }
            ],
            "input": [
                {
                    "name": "IDToken2",
                    "value": ""
                }
            ],
            "_id": 1
        }""")
        val passwordCallback = PasswordCallback(raw, 0)
        Assert.assertEquals("Password", passwordCallback.getPrompt())
        passwordCallback.setPassword("tester".toCharArray())
        Assert.assertEquals((passwordCallback.contentAsJson.getJSONArray("input")[0] as JSONObject).getString(
            "value"),
            "tester")
        Assert.assertEquals(1, passwordCallback.get_id().toLong())
    }
}