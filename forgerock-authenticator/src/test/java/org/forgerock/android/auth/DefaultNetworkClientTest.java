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

import org.forgerock.android.authenticator.FRAListenerFuture;
import org.forgerock.android.authenticator.network.ConnectionProperties;
import org.forgerock.android.authenticator.network.ConnectionResponse;
import org.forgerock.android.authenticator.network.RequestMethod;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.forgerock.android.authenticator.network.ConnectionProperties.CONTENT_TYPE;
import static org.forgerock.android.authenticator.network.ConnectionProperties.DEFAULT_CONTENT_TYPE;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class DefaultNetworkClientTest {

    private Context context = ApplicationProvider.getApplicationContext();
    private DefaultNetworkClient networkClient;

    @Rule
    public final MockWebServer mockWebServer = new MockWebServer();

    @Test
    public void testConnectSynchronously() throws Exception {
        mockWebServer.enqueue(new MockResponse());
        Map<String, String> headerParameters = new HashMap<>();
        headerParameters.put(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        headerParameters.put("header1", "header1");

        URL url = mockWebServer.url("/").url();
        ConnectionProperties properties = new ConnectionProperties.PropertyBuilder()
                .setRequestMethod(RequestMethod.POST)
                .setHeaderParameters(headerParameters)
                .build();

        DefaultNetworkClient networkClient = new DefaultNetworkClient(context);
        ConnectionResponse connectionResponse = networkClient.connect(url, properties);

        int code = connectionResponse.getResponseCode();
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(code, HTTP_OK);
        assertEquals(recordedRequest.getHeader(CONTENT_TYPE), DEFAULT_CONTENT_TYPE);
        assertEquals(recordedRequest.getHeader("header1"), "header1");
    }

    @Test
    public void testConnectAsynchronously() throws Exception {
        mockWebServer.enqueue(new MockResponse());
        Map<String, String> headerParameters = new HashMap<>();
        headerParameters.put(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        headerParameters.put("header1", "header1");

        URL url = mockWebServer.url("/").url();
        ConnectionProperties properties = new ConnectionProperties.PropertyBuilder()
                .setRequestMethod(RequestMethod.POST)
                .setHeaderParameters(headerParameters)
                .build();

        DefaultNetworkClient networkClient = new DefaultNetworkClient(context);
        FRAListenerFuture<ConnectionResponse> listener = new FRAListenerFuture<>();
        networkClient.connect(url, properties, listener);

        ConnectionResponse connectionResponse = listener.get();

        int code = connectionResponse.getResponseCode();
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(code, HTTP_OK);
        assertEquals(recordedRequest.getHeader(CONTENT_TYPE), DEFAULT_CONTENT_TYPE);
        assertEquals(recordedRequest.getHeader("header1"), "header1");
    }

    @Test
    public void testTerminate() {
        DefaultNetworkClient networkClient = new DefaultNetworkClient(context);
        Exception exception = null;

        try {
            networkClient.terminate();
        } catch (Exception e) {
            exception = e;
        }

        assertNotNull(exception);
        assertEquals(exception.getClass(), UnsupportedOperationException.class);
    }

    @Test
    public void testCancel() {
        String sampleJson = "{\n" +
                "    \"fruit\": \"Apple\",\n" +
                "    \"size\": \"Large\",\n" +
                "    \"color\": \"Red\"\n" +
                "}";

        URL url = mockWebServer.url("/").url();
        ConnectionProperties properties = new ConnectionProperties.PropertyBuilder()
                .setRequestMethod(RequestMethod.POST)
                .setHeaderParameter(CONTENT_TYPE, DEFAULT_CONTENT_TYPE)
                .build();

        MockResponse response = new MockResponse().setResponseCode(HTTP_OK)
                .setBody(sampleJson)
                .setBodyDelay(5, TimeUnit.SECONDS);

        mockWebServer.enqueue(response);

        DefaultNetworkClient networkClient = new DefaultNetworkClient(context);

        final ConnectionResponse[] connectionResponse = new ConnectionResponse[1];
        final Exception[] exception = new Exception[1];

        Thread t = new Thread(() -> {
            try {
                connectionResponse[0] = networkClient.connect(url, properties);
            } catch (Exception e) {
                exception[0] = e;
            }
        });

        t.start();
        networkClient.cancel();

        if (exception[0] != null) {
            //The exception can be canceled or stream is closed.
            String errorMessage = exception[0].getMessage();
            assertTrue("Canceled".equals(errorMessage) || "stream is closed".equals(errorMessage));
        } else {
            assertNull(connectionResponse[0]);
        }

    }

}