/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
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

@RunWith(RobolectricTestRunner.class)
public class MetadataCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"MetadataCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"data\",\n" +
                "                    \"value\": {\n" +
                "                        \"stage\": \"UsernamePassword\"\n" +
                "                    }\n" +
                "                }\n" +
                "            ],\n" +
                "            \"_id\": 0\n" +
                "        }");
        MetadataCallback callback = new MetadataCallback(raw, 0);
        assertThat(callback.getValue()).isInstanceOf(JSONObject.class);
        assertThat(callback.getValue().getString("stage")).isEqualTo("UsernamePassword");
        assertThat(callback.get_id()).isEqualTo(0);

    }
}