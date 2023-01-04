/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectIdPCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "SelectIdPCallback",
            "output": [
                {
                    "name": "providers",
                    "value": [
                        {
                            "provider": "google",
                            "uiConfig": {
                                "buttonImage": "images/g-logo.png",
                                "buttonCustomStyle": "background-color: #fff; color: #757575; border-color: #ddd;",
                                "abc": "test",
                                "buttonClass": "",
                                "buttonDisplayName": "Google",
                                "buttonCustomStyleHover": "color: #6d6d6d; background-color: #eee; border-color: #ccc;",
                                "iconClass": "fa-google",
                                "userinfo": "https://www.googleapis.com/oauth2/v3/userinfo",
                                "iconFontColor": "white",
                                "iconBackground": "#4184f3"
                            }
                        },
                        {
                            "provider": "facebook",
                            "uiConfig": {
                                "buttonImage": "",
                                "buttonCustomStyle": "background-color: #3b5998;border-color: #3b5998; color: white;",
                                "buttonClass": "fa-facebook-official",
                                "buttonDisplayName": "Facebook",
                                "buttonCustomStyleHover": "background-color: #334b7d;border-color: #334b7d; color: white;",
                                "iconFontColor": "white",
                                "iconClass": "fa-facebook",
                                "iconBackground": "#3b5998"
                            }
                        },
                        {
                            "provider": "localAuthentication"
                        }
                    ]
                },
                {
                    "name": "value",
                    "value": ""
                }
            ],
            "input": [
                {
                    "name": "IDToken1",
                    "value": ""
                }
            ]
        }""")
        val callback = SelectIdPCallback(raw, 0)
        Assertions.assertThat(callback.providers).hasSize(3)
        Assertions.assertThat(callback.providers[0].provider).isEqualTo("google")
        Assertions.assertThat(JSONObject(callback.providers[0].uiConfig).getString("buttonDisplayName"))
            .isEqualTo("Google")
        Assertions.assertThat(callback.providers[1].provider).isEqualTo("facebook")
        Assertions.assertThat(JSONObject(callback.providers[1].uiConfig).getString("buttonDisplayName"))
            .isEqualTo("Facebook")
        Assertions.assertThat(callback.providers[2].provider).isEqualTo("localAuthentication")
        callback.setValue("google")
        Assert.assertEquals((callback.contentAsJson.getJSONArray("input")[0] as JSONObject).getString(
            "value"),
            "google")
        Assert.assertEquals(0, callback.get_id().toLong())
    }
}