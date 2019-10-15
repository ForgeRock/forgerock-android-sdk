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
public class PollingWaitCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"PollingWaitCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"waitTime\",\n" +
                "                    \"value\": \"30000\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"message\",\n" +
                "                    \"value\": \"Please Wait. Processing ...\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        PollingWaitCallback pollingWaitCallback = new PollingWaitCallback(raw, 0);
        assertEquals("Please Wait. Processing ...", pollingWaitCallback.getMessage());
        assertEquals("30000", pollingWaitCallback.getWaitTime());

    }
}