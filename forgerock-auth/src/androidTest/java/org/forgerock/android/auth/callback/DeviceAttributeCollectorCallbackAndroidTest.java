/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import android.Manifest;
import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.forgerock.android.auth.FRListenerFuture;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DeviceAttributeCollectorCallbackAndroidTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH
    );

    @Test
    public void testProfile() throws Exception {


        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"HiddenValueCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"value\",\n" +
                "                    \"value\": \"\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"id\",\n" +
                "                    \"value\": \"DeviceAttributeCallback://forgerock?attributes=profile\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": \"DeviceAttributeCallback://forgerock?attributes=profile\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        DeviceAttributeCallback callback = new DeviceAttributeCallback(raw, 0);
        FRListenerFuture<Void> result = new FRListenerFuture<>();
        callback.execute(context, result);
        result.get();

        String content = ((JSONObject)callback.getContentAsJson().getJSONArray("input").get(0)).getString("value");

        assertTrue(content.contains("identifier"));
        assertTrue(content.contains("profile"));
    }

    @Test
    public void testLocation() throws Exception {

        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"HiddenValueCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"value\",\n" +
                "                    \"value\": \"\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"id\",\n" +
                "                    \"value\": \"DeviceAttributeCallback://forgerock?attributes=location\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": \"DeviceAttributeCallback://forgerock?attributes=location\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        DeviceAttributeCallback callback = new DeviceAttributeCallback(raw, 0);
        FRListenerFuture<Void> result = new FRListenerFuture<>();
        callback.execute(context, result);
        result.get();

        String content = ((JSONObject)callback.getContentAsJson().getJSONArray("input").get(0)).getString("value");

        assertTrue(content.contains("identifier"));

        if (!isEmulator()) {
            assertTrue(content.contains("location"));
        }
    }

    private boolean isEmulator() {
        return Build.PRODUCT.matches(".*_?sdk_?.*");
    }


}