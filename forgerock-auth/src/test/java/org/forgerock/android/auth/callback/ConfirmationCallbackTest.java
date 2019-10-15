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
public class ConfirmationCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"ConfirmationCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"prompt\",\n" +
                "                    \"value\": \"\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"messageType\",\n" +
                "                    \"value\": 0\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"options\",\n" +
                "                    \"value\": [\n" +
                "                        \"I can't wait, please let me go.\"\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"optionType\",\n" +
                "                    \"value\": -1\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"defaultOption\",\n" +
                "                    \"value\": 0\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken2\",\n" +
                "                    \"value\": 100\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        ConfirmationCallback confirmationCallback = new ConfirmationCallback(raw, 0);
        assertEquals("", confirmationCallback.getPrompt());
        assertEquals(ConfirmationCallback.INFORMATION, confirmationCallback.getMessageType());
        assertEquals("I can't wait, please let me go.", confirmationCallback.getOptions().get(0));
        assertEquals(ConfirmationCallback.UNSPECIFIED_OPTION, confirmationCallback.getOptionType());
        assertEquals(ConfirmationCallback.YES, confirmationCallback.getDefaultOption());

        confirmationCallback.setSelectedIndex(0);

        assertEquals(0, ((JSONObject)confirmationCallback.getContentAsJson().getJSONArray("input").get(0)).getInt("value"));


    }
}