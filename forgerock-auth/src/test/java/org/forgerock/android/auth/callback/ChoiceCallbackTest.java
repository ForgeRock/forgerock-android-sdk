/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
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

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ChoiceCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"ChoiceCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"prompt\",\n" +
                "                    \"value\": \"Second Factor\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"choices\",\n" +
                "                    \"value\": [\n" +
                "                        \"email\",\n" +
                "                        \"sms\"\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"defaultChoice\",\n" +
                "                    \"value\": 0\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": 0\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        ChoiceCallback choiceCallback = new ChoiceCallback(raw, 0);
        assertEquals("Second Factor", choiceCallback.getPrompt());

        choiceCallback.setSelectedIndex(1);
        assertEquals(1, ((JSONObject)choiceCallback.getContentAsJson().getJSONArray("input").get(0)).getInt("value"));

    }
}