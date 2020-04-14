/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RESTClient {

    /** The CONTENT_TYPE key */
    public static final String CONTENT_TYPE = "Content-Type";

    /** The DEFAULT_CONTENT_TYPE value */
    public static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    /** The COOKIE key */
    public static final String COOKIE = "Cookie";

    /** The `Call` represents the request that will be prepared for execution */
    protected volatile Call call;

    /** The `Response` represents the HTTP response */
    private Response response;

    /** Any exception on executing the REST API call */
    private Exception exception;

    /** The application Context */
    protected static Context context;

    /**
     * A REST API client implementation using OkHttpClient.
     * @param context application context
     */
    public RESTClient(Context context) {
        this.context = context;
    }

    /**
     * The `OkHttpClient` instance used to make the HTTP calls
     *
     * @return OkHttpClient http client
     * */
    private OkHttpClient getOkHttpClient(URL url) {

        ServerConfig serverConfig = ServerConfig.builder()
                .url(url.getProtocol() + "://" + url.getAuthority())
                .context(this.context)
                .build();

        return OkHttpClientProvider.getInstance().lookup(serverConfig);
    }

    /**
     * Build a request.
     *
     * @param url   the endpoint url
     * @param amlbCookie the am load balance cookie
     * @param message the data to attach to the response.
     * @return the response code of the request
     */
    private Request buildRequest(URL url, String amlbCookie, JSONObject message) {
        Request.Builder requestBuilder = new Request.Builder().url(url);

        // Add header properties to the request
        requestBuilder.addHeader(CONTENT_TYPE, JSON_CONTENT_TYPE);
        requestBuilder.addHeader(ServerConfig.ACCEPT_API_VERSION, ServerConfig.API_VERSION_1_0);
        if (amlbCookie != null) {
            requestBuilder.addHeader(COOKIE, amlbCookie);
        }

        // Add body parameters to the request
        RequestBody body = RequestBody.create(message.toString(),
                MediaType.parse("application/json; charset=utf-8"));
        requestBuilder.post(body);

        return requestBuilder.build();
    }

    public int invokeEndpoint(@NonNull URL url, String amlbCookie, @NonNull JSONObject message) throws Exception {
        OkHttpClient okHttpClient = getOkHttpClient(url);
        Request request = buildRequest(url, amlbCookie, message);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                exception = e;
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response r) {
                response = r;
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();

        if (exception != null) {
            throw exception;
        }

        return response.code();
    }

}
