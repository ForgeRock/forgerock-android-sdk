/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator.network;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.forgerock.android.authenticator.network.ConnectionProperties.CONTENT_TYPE;
import static org.forgerock.android.authenticator.network.ConnectionProperties.DEFAULT_CONTENT_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class ConnectionResponseTest {

    private Context context = ApplicationProvider.getApplicationContext();
    private Response response;

    @Rule
    public final MockWebServer mockWebServer = new MockWebServer();

    @Before
    public void setUp() {
        Request mockRequest = new Request.Builder()
                .url("https://openam.forgerock.com:8080/openam/json/push/sns/message?_action=register")
                .build();
        response = new Response.Builder()
                .request(mockRequest)
                .protocol(Protocol.HTTP_1_1)
                .addHeader(CONTENT_TYPE, DEFAULT_CONTENT_TYPE)
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Accept-Language", "en-gb")
                .code(HTTP_OK) // status code
                .message("OK")
                .body(ResponseBody.create("{}",
                        MediaType.get("application/json; charset=utf-8")))
                .build();
    }

    @Test
    public void testCreateConnectionReponseUsingMultiplesParameters() {
        ConnectionResponse connectionReponse = new ConnectionResponse(response.headers().toMultimap(),
                response.code(), response.message(), (int) response.body().contentLength(), response.body().byteStream());

        assertEquals(connectionReponse.getResponseCode(), HTTP_OK);
        assertEquals(connectionReponse.getResponseMessage(), response.message());
        assertEquals(connectionReponse.getContentLength(), response.body().contentLength());
        assertNotNull(connectionReponse.getBody());
        assertEquals(connectionReponse.getAllHeaders(), response.headers().toMultimap());
    }

    @Test
    public void testCreateConnectionReponseUsingReponseParameter() {
        ConnectionResponse connectionReponse = new ConnectionResponse(response);

        assertEquals(connectionReponse.getResponseCode(), HTTP_OK);
        assertEquals(connectionReponse.getResponseMessage(), response.message());
        assertEquals(connectionReponse.getContentLength(), response.body().contentLength());
        assertNotNull(connectionReponse.getBody());
        assertEquals(connectionReponse.getAllHeaders(), response.headers().toMultimap());
    }

    @Test
    public void getContentLength() {
        ConnectionResponse connectionReponse = new ConnectionResponse(response);

        assertEquals(connectionReponse.getContentLength(), response.body().contentLength());
    }

    @Test
    public void getAllHeaders() {
        ConnectionResponse connectionReponse = new ConnectionResponse(response);

        assertEquals(connectionReponse.getAllHeaders(), response.headers().toMultimap());
    }

    @Test
    public void getResponseCode() {
        ConnectionResponse connectionReponse = new ConnectionResponse(response);

        assertEquals(connectionReponse.getResponseCode(), HTTP_OK);
    }

    @Test
    public void getResponseMessage() {
        ConnectionResponse connectionReponse = new ConnectionResponse(response);

        assertEquals(connectionReponse.getResponseMessage(), response.message());
    }

    @Test
    public void getBody() {
        ConnectionResponse connectionReponse = new ConnectionResponse(response);

        assertNotNull(connectionReponse.getBody());
    }

}