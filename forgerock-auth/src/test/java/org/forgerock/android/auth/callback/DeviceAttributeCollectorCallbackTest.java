/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.Config;
import org.forgerock.android.auth.FRListenerFuture;
import org.forgerock.android.auth.KeyStoreManager;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.security.PublicKey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DeviceAttributeCollectorCallbackTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Mock
    public KeyStoreManager keyStoreManager;

    @Mock
    public PublicKey publicKey;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testProfile() throws Exception {

        byte[] encoded = "public key".getBytes();
        when(publicKey.getEncoded()).thenReturn(encoded);
        when(keyStoreManager.getIdentifierKey(any())).thenReturn(publicKey);

        Config.getInstance(context).setKeyStoreManager(keyStoreManager);


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

        Assertions.assertThat(content).contains("identifier").contains("profile");
    }

    @Test
    public void testNoAttributesToCollect() throws Exception {

        byte[] encoded = "public key".getBytes();
        when(publicKey.getEncoded()).thenReturn(encoded);
        when(keyStoreManager.getIdentifierKey(any())).thenReturn(publicKey);

        Config.getInstance(context).setKeyStoreManager(keyStoreManager);


        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"HiddenValueCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"value\",\n" +
                "                    \"value\": \"\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"id\",\n" +
                "                    \"value\": \"DeviceAttributeCallback://forgerock\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": \"DeviceAttributeCallback://forgerock\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        DeviceAttributeCallback callback = new DeviceAttributeCallback(raw, 0);
        FRListenerFuture<Void> result = new FRListenerFuture<>();
        callback.execute(context, result);
        result.get();

        String content = ((JSONObject)callback.getContentAsJson().getJSONArray("input").get(0)).getString("value");

        Assertions.assertThat(content).contains("identifier");
    }

    @Test
    public void testAttributesToCollect() throws Exception {

        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"HiddenValueCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"value\",\n" +
                "                    \"value\": \"\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"id\",\n" +
                "                    \"value\": \"DeviceAttributeCallback://forgerock?attributes=profile&attributes=publicKey&attributes=location\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": \"DeviceAttributeCallback://forgerock?attributes=profile&attributes=publicKey&attributes=location\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");

        DeviceAttributeCallback callback = new DeviceAttributeCallback(raw, 0);
        Assertions.assertThat(callback.getAttributes())
                .contains("profile")
                .contains("publicKey")
                .contains("location");

    }



}