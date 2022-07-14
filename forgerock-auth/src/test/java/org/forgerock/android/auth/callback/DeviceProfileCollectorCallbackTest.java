/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

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

@RunWith(RobolectricTestRunner.class)
public class DeviceProfileCollectorCallbackTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Mock
    public KeyStoreManager keyStoreManager;

    @Mock
    public PublicKey publicKey;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        byte[] encoded = "public key".getBytes();
        when(publicKey.getEncoded()).thenReturn(encoded);
        when(keyStoreManager.getIdentifierKey(any())).thenReturn(publicKey);
        Config.getInstance().init(context, null);
        Config.getInstance().setKeyStoreManager(keyStoreManager);
    }

    @Test
    public void testMetadata() throws Exception {

        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"DeviceProfileCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"metadata\",\n" +
                "                    \"value\": true\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"location\",\n" +
                "                    \"value\": false\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"message\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        DeviceProfileCallback callback = new DeviceProfileCallback(raw, 0);
        FRListenerFuture<Void> result = new FRListenerFuture<>();
        callback.execute(context, result);
        result.get();

        String content = ((JSONObject) callback.getContentAsJson().getJSONArray("input").get(0)).getString("value");

        JSONObject contentAsJson = new JSONObject(content);
        assertThat(contentAsJson.get("identifier")).isNotNull();
        assertThat(contentAsJson.get("metadata")).isNotNull();
    }

    @Test
    public void testNoAttributesToCollect() throws Exception {

        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"DeviceProfileCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"metadata\",\n" +
                "                    \"value\": false\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"location\",\n" +
                "                    \"value\": false\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"message\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        DeviceProfileCallback callback = new DeviceProfileCallback(raw, 0);
        FRListenerFuture<Void> result = new FRListenerFuture<>();
        callback.execute(context, result);
        result.get();

        String content = ((JSONObject) callback.getContentAsJson().getJSONArray("input").get(0)).getString("value");

        JSONObject contentAsJson = new JSONObject(content);
        assertThat(contentAsJson.get("identifier")).isNotNull();
        assertThat(contentAsJson.opt("metadata")).isNull();
        assertThat(contentAsJson.opt("location")).isNull();

    }


    @Test
    public void testAttributesToCollect() throws Exception {

        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"DeviceProfileCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"metadata\",\n" +
                "                    \"value\": true\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"location\",\n" +
                "                    \"value\": true\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"message\",\n" +
                "                    \"value\": \"Test Message\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");

        DeviceProfileCallback callback = new DeviceProfileCallback(raw, 0);
        assertThat(callback.isMetadata()).isTrue();
        assertThat(callback.isLocation()).isTrue();
        assertThat(callback.getMessage()).isEqualTo("Test Message");

    }


}