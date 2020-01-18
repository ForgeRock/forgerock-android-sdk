/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class NameCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"NameCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"prompt\",\n" +
                "                    \"value\": \"User Name\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"_id\": 0\n" +
                "        }");
        NameCallback nameCallback = new NameCallback(raw, 0);
        assertEquals("User Name", nameCallback.getPrompt());

        nameCallback.setName("tester");
        assertEquals(((JSONObject)nameCallback.getContentAsJson().getJSONArray("input").get(0)).getString("value"),
                "tester");

        assertEquals(0, nameCallback.get_id());

    }
}