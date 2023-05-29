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
class ConsentMappingCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "ConsentMappingCallback",
            "output": [
                {
                    "name": "name",
                    "value": "managedUser_systemLdapAccounts"
                },
                {
                    "name": "displayName",
                    "value": "ForgeRock IDM"
                },
                {
                    "name": "icon",
                    "value": "https://www.forgerock.com/sites/default/files/2018-04/platform-am-nolabel.png"
                },
                {
                    "name": "accessLevel",
                    "value": "Actual Profile"
                },
                {
                    "name": "isRequired",
                    "value": false
                },
                {
                    "name": "message",
                    "value": "Test Message"
                },
                {
                    "name": "fields",
                    "value": [
                        "First Name",
                        "Last Name",
                        "Username",
                        "Description",
                        "Email Address",
                        "Password",
                        "Telephone Number",
                        null
                    ]
                }
            ],
            "input": [
                {
                    "name": "IDToken1",
                    "value": false
                }
            ]
        }""")
        val callback = ConsentMappingCallback(raw, 0)
        Assert.assertEquals("managedUser_systemLdapAccounts", callback.name)
        Assert.assertEquals("ForgeRock IDM", callback.displayName)
        Assert.assertEquals("https://www.forgerock.com/sites/default/files/2018-04/platform-am-nolabel.png",
            callback.icon)
        Assert.assertEquals("Actual Profile", callback.accessLevel)
        Assert.assertFalse(callback.isRequired)
        Assert.assertEquals("First Name", callback.fields[0])
        Assert.assertEquals("Test Message", callback.message)
        callback.setAccept(true)
        Assert.assertTrue((callback.contentAsJson.getJSONArray("input")[0] as JSONObject).getBoolean(
            "value"))
    }
}