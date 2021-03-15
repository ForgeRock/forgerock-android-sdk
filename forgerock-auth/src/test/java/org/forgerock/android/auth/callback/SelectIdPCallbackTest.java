/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class SelectIdPCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"SelectIdPCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"providers\",\n" +
                "                    \"value\": [\n" +
                "                        {\n" +
                "                            \"provider\": \"google\",\n" +
                "                            \"uiConfig\": {\n" +
                "                                \"buttonImage\": \"images/g-logo.png\",\n" +
                "                                \"buttonCustomStyle\": \"background-color: #fff; color: #757575; border-color: #ddd;\",\n" +
                "                                \"abc\": \"test\",\n" +
                "                                \"buttonClass\": \"\",\n" +
                "                                \"buttonDisplayName\": \"Google\",\n" +
                "                                \"buttonCustomStyleHover\": \"color: #6d6d6d; background-color: #eee; border-color: #ccc;\",\n" +
                "                                \"iconClass\": \"fa-google\",\n" +
                "                                \"userinfo\": \"https://www.googleapis.com/oauth2/v3/userinfo\",\n" +
                "                                \"iconFontColor\": \"white\",\n" +
                "                                \"iconBackground\": \"#4184f3\"\n" +
                "                            }\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"provider\": \"facebook\",\n" +
                "                            \"uiConfig\": {\n" +
                "                                \"buttonImage\": \"\",\n" +
                "                                \"buttonCustomStyle\": \"background-color: #3b5998;border-color: #3b5998; color: white;\",\n" +
                "                                \"buttonClass\": \"fa-facebook-official\",\n" +
                "                                \"buttonDisplayName\": \"Facebook\",\n" +
                "                                \"buttonCustomStyleHover\": \"background-color: #334b7d;border-color: #334b7d; color: white;\",\n" +
                "                                \"iconFontColor\": \"white\",\n" +
                "                                \"iconClass\": \"fa-facebook\",\n" +
                "                                \"iconBackground\": \"#3b5998\"\n" +
                "                            }\n" +
                "                        },\n" +
                "                        {\n" +
                "                            \"provider\": \"localAuthentication\"\n" +
                "                        }\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"value\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        SelectIdPCallback callback = new SelectIdPCallback(raw, 0);
        assertThat(callback.getProviders()).hasSize(3);
        assertThat(callback.getProviders().get(0).getProvider()).isEqualTo("google");
        assertThat(new JSONObject(callback.getProviders().get(0).getUiConfig()).getString("buttonDisplayName"))
                .isEqualTo("Google");
        assertThat(callback.getProviders().get(1).getProvider()).isEqualTo("facebook");
        assertThat(new JSONObject(callback.getProviders().get(1).getUiConfig()).getString("buttonDisplayName"))
                .isEqualTo("Facebook");
        assertThat(callback.getProviders().get(2).getProvider()).isEqualTo("localAuthentication");

        callback.setValue("google");
        assertEquals(((JSONObject)callback.getContentAsJson().getJSONArray("input").get(0)).getString("value"),
                "google");

        assertEquals(0, callback.get_id());

    }
}