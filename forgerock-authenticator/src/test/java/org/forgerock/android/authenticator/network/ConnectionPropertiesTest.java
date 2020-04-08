/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator.network;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import com.squareup.okhttp.mockwebserver.SocketPolicy;

import org.forgerock.android.auth.DefaultNetworkClient;
import org.forgerock.android.authenticator.network.ConnectionProperties.PropertyBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.forgerock.android.authenticator.network.ConnectionProperties.CONTENT_TYPE;
import static org.forgerock.android.authenticator.network.ConnectionProperties.DEFAULT_CONTENT_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ConnectionPropertiesTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Rule
    public final MockWebServer mockWebServer = new MockWebServer();

    @Test
    public void testRequestMethod() {
        ConnectionProperties connectionProperties = getConnectionProperties();
        assertEquals(connectionProperties.requestMethod(), RequestMethod.POST);
    }

    @Test
    public void testHeaderParameters() {
        ConnectionProperties connectionProperties = getConnectionProperties();
        Map<String, String> prop = connectionProperties.headerParameters();
        assertTrue(prop.containsKey("header_key_1"));
        assertTrue(prop.containsKey("header_key_2"));
    }

    @Test
    public void testBodyParameters() {
        ConnectionProperties connectionProperties = getConnectionProperties();
        Map<String, String> post = connectionProperties.bodyParameters();
        assertTrue(post.containsKey("body_key_1"));
        assertTrue(post.containsKey("body_key_2"));
    }

    @Test
    public void testConnectionTimeout() {
        ConnectionProperties connectionProperties = getConnectionProperties();
        assertEquals(connectionProperties.connectionTimeout(), 5000);
    }

    @Test
    public void testBuilderWithHeaderParameters() throws Exception {
        mockWebServer.enqueue(new MockResponse());
        Map<String, String> headerParameters = new HashMap<>();
        headerParameters.put(CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        headerParameters.put("header1", "header1");

        URL url = mockWebServer.url("/").url();
        ConnectionProperties properties = new PropertyBuilder()
                .setRequestMethod(RequestMethod.GET)
                .setHeaderParameters(headerParameters)
                .setHeaderParameter("header2", "header2")
                .build();

        DefaultNetworkClient networkClient = new DefaultNetworkClient(context);
        ConnectionResponse connectionResponse = networkClient.connect(url, properties);

        int code = connectionResponse.getResponseCode();
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals(code, HTTP_OK);
        assertEquals(recordedRequest.getHeader(CONTENT_TYPE), DEFAULT_CONTENT_TYPE);
        assertEquals(recordedRequest.getHeader("header1"), "header1");
        assertEquals(recordedRequest.getHeader("header2"), "header2");
    }

    @Test
    public void testBuilderWithBodyParameters() throws Exception {
        mockWebServer.enqueue(new MockResponse());
        Map<String, String> bodyParameters = new HashMap<>();
        bodyParameters.put("body1", "body1");
        bodyParameters.put("body2", "body2");

        URL url = mockWebServer.url("/").url();
        ConnectionProperties properties = new PropertyBuilder()
                .setRequestMethod(RequestMethod.POST)
                .setBodyParameters(bodyParameters)
                .setBodyParameter("body3", "body3")
                .build();

        DefaultNetworkClient networkClient = new DefaultNetworkClient(context);
        ConnectionResponse connectionResponse = networkClient.connect(url, properties);

        int code = connectionResponse.getResponseCode();
        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        String requestBody = recordedRequest.getBody().readUtf8Line();
        assertEquals(code, HTTP_OK);
        assertTrue(requestBody.contains("body1=body1"));
        assertTrue(requestBody.contains("body2=body2"));
        assertTrue(requestBody.contains("body3=body3"));
    }

    @Test
    public void testBuilderWithTimeOut() throws Exception {
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        int start = Calendar.getInstance().get(Calendar.SECOND);

        URL url = mockWebServer.url("/").url();
        ConnectionProperties properties = new PropertyBuilder()
                .setRequestMethod(RequestMethod.GET)
                .setConnectionTimeout(2)
                .build();

        DefaultNetworkClient networkClient = new DefaultNetworkClient(context);
        ConnectionResponse connectionResponse = null;
        try {
            connectionResponse = networkClient.connect(url, properties);
        } catch (SocketTimeoutException ex) {
        }

        int end = Calendar.getInstance().get(Calendar.SECOND);

        assertEquals(start, (end - 2));
    }

    private ConnectionProperties getConnectionProperties() {
        ConnectionProperties connectionProperties = new PropertyBuilder()
                .setRequestMethod(RequestMethod.POST)
                .setConnectionTimeout(5000)
                .setHeaderParameter("header_key_1", "header_value_1")
                .setBodyParameter("body_key_1", "body_value_1")
                .setHeaderParameters(Collections.singletonMap("header_key_2", "header_value_2"))
                .setBodyParameters(Collections.singletonMap("body_key_2", "body_value_2"))
                .build();

        return connectionProperties;
    }
}