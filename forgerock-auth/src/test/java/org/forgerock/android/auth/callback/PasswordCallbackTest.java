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
public class PasswordCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"PasswordCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"prompt\",\n" +
                "                    \"value\": \"Password\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken2\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"_id\": 1\n" +
                "        }");
        PasswordCallback passwordCallback = new PasswordCallback(raw, 0);
        assertEquals("Password", passwordCallback.getPrompt());

        passwordCallback.setPassword("tester".toCharArray());
        assertEquals(((JSONObject)passwordCallback.getContentAsJson().getJSONArray("input").get(0)).getString("value"),
                "tester");

        assertEquals(1, passwordCallback.get_id());

    }
}