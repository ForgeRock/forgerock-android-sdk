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
public class TextOutputCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"TextOutputCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"message\",\n" +
                "                    \"value\": \"This is a Message Node\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"messageType\",\n" +
                "                    \"value\": \"1\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        TextOutputCallback callback = new TextOutputCallback(raw, 0);
        assertEquals("This is a Message Node", callback.getMessage());
        assertEquals(1, callback.getMessageType());

    }
}