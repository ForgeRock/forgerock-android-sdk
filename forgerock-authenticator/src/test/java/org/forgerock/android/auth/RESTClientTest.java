/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.URL;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class RESTClientTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Rule
    public final MockWebServer mockWebServer = new MockWebServer();

    @Test
    public void testInvokeEndpointSuccess() throws Exception {
        mockWebServer.enqueue(new MockResponse());

        URL url = mockWebServer.url("/").url();

        JSONObject message = new JSONObject();
        message.put("messageId", "1");
        message.put("jwt", "1");

        RESTClient client = new RESTClient(context);
        int code = client.invokeEndpoint(url, null, message);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(code, HTTP_OK);
        assertEquals(recordedRequest.getHeader(RESTClient.CONTENT_TYPE), RESTClient.JSON_CONTENT_TYPE);
        assertEquals(recordedRequest.getHeader(ServerConfig.ACCEPT_API_VERSION), ServerConfig.API_VERSION_1_0);
    }

    @Test
    public void testInvokeEndpointFail() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(HTTP_NOT_FOUND));

        URL url = mockWebServer.url("/").url();

        JSONObject message = new JSONObject();
        message.put("messageId", "1");
        message.put("jwt", "1");

        RESTClient client = new RESTClient(context);
        int code = client.invokeEndpoint(url, null, message);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(code, HTTP_NOT_FOUND);
        assertEquals(recordedRequest.getHeader(RESTClient.CONTENT_TYPE), RESTClient.JSON_CONTENT_TYPE);
        assertEquals(recordedRequest.getHeader(ServerConfig.ACCEPT_API_VERSION), ServerConfig.API_VERSION_1_0);
    }

}