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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ConsentMappingCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"ConsentMappingCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"name\",\n" +
                "                    \"value\": \"managedUser_systemLdapAccounts\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"displayName\",\n" +
                "                    \"value\": \"ForgeRock IDM\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"icon\",\n" +
                "                    \"value\": \"https://www.forgerock.com/sites/default/files/2018-04/platform-am-nolabel.png\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"accessLevel\",\n" +
                "                    \"value\": \"Actual Profile\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"isRequired\",\n" +
                "                    \"value\": false\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"message\",\n" +
                "                    \"value\": \"Test Message\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"fields\",\n" +
                "                    \"value\": [\n" +
                "                        \"First Name\",\n" +
                "                        \"Last Name\",\n" +
                "                        \"Username\",\n" +
                "                        \"Description\",\n" +
                "                        \"Email Address\",\n" +
                "                        \"Password\",\n" +
                "                        \"Telephone Number\",\n" +
                "                        null\n" +
                "                    ]\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": false\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        ConsentMappingCallback callback = new ConsentMappingCallback(raw, 0);
        assertEquals("managedUser_systemLdapAccounts", callback.getName());
        assertEquals("ForgeRock IDM", callback.getDisplayName());
        assertEquals("https://www.forgerock.com/sites/default/files/2018-04/platform-am-nolabel.png", callback.getIcon());
        assertEquals("Actual Profile", callback.getAccessLevel());
        assertFalse(callback.isRequired());
        assertEquals("First Name", callback.getFields()[0]);
        assertEquals("Test Message", callback.getMessage());

        callback.setAccept(true);

        assertTrue(((JSONObject) callback.getContentAsJson().getJSONArray("input").get(0)).getBoolean("value"));


    }
}