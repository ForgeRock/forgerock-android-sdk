/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.annotation.NonNull;

import org.forgerock.android.authenticator.FRAListener;
import org.forgerock.android.authenticator.network.ConnectionProperties;
import org.forgerock.android.authenticator.network.ConnectionResponse;
import org.forgerock.android.authenticator.network.NetworkClient;
import org.forgerock.android.authenticator.network.RequestMethod;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DefaultNetworkClient implements NetworkClient {

    /** The `Call` represents the request that will be prepared for execution */
    protected volatile Call call;

    /** The `Response` represents the HTTP response */
    private Response response;

    /** Any exception on executing the network call */
    private Exception exception;

    /** The application Context */
    protected static Context context;

    /**
     * A default NetworkClient implementation using OkHttpClient.
     * @param context application context
     */
    public DefaultNetworkClient(Context context) {
        this.context = context;
    }

    @Override
    public ConnectionResponse connect(@NonNull URL url, @NonNull ConnectionProperties connectionProperties) throws Exception {

        OkHttpClient okHttpClient = getOkHttpClient(url, connectionProperties);
        Request request = buildRequest(url, connectionProperties);

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

        return new ConnectionResponse(response);
    }

    @Override
    public void connect(@NonNull URL url, @NonNull ConnectionProperties connectionProperties, @NonNull FRAListener<ConnectionResponse> listener) {

        OkHttpClient okHttpClient = getOkHttpClient(url, connectionProperties);
        Request request = buildRequest(url, connectionProperties);

        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onException(e);
            }

            @Override
            public void onResponse(Call call, Response r) {
                ConnectionResponse response = new ConnectionResponse(r);
                listener.onSuccess(response);
            }
        });
    }

    /**
     * The `OkHttpClient` instance used to make the HTTP calls
     *
     * @param connectionProperties the network connection properties
     * @return OkHttpClient http client
     * */
    private OkHttpClient getOkHttpClient(URL url, ConnectionProperties connectionProperties) {

        ServerConfig serverConfig = ServerConfig.builder()
                .url(url.getProtocol() + "://" + url.getAuthority())
                .context(this.context)
                .timeout(connectionProperties.connectionTimeout())
                .build();

        return OkHttpClientProvider.getInstance().lookup(serverConfig);
    }
    /**
     * Build a request.
     *
     * @param url   the endpoint url
     * @param connectionProperties the connections properties
     * @return the request object
     */
    private Request buildRequest(URL url, ConnectionProperties connectionProperties) {
        Request.Builder requestBuilder = new Request.Builder().url(url);

        // Add header properties to the request
        Map<String, String> headerParameters = connectionProperties.headerParameters();
        if (headerParameters != null) {
            for (Map.Entry<String, String> headerEntry : headerParameters.entrySet()) {
                String key = headerEntry.getKey();
                requestBuilder.addHeader(key, headerEntry.getValue());
            }
        }

        // Add body parameters to the request according to the RequestType (GET or POST)
        if (connectionProperties.requestMethod() == RequestMethod.POST) {
            Map<String, String> bodyParameters = connectionProperties.bodyParameters();
            if (bodyParameters != null) {
                FormBody.Builder builder = new FormBody.Builder();
                for (Map.Entry<String, String> postEntry : bodyParameters.entrySet()) {
                    String key = postEntry.getKey();
                    builder.add(key, postEntry.getValue());
                }
                RequestBody requestBody = builder.build();
                requestBuilder.post(requestBody);
            } else {
                requestBuilder.post(RequestBody.create("", null));
            }
        } else {
            requestBuilder = requestBuilder.get();
        }

        return requestBuilder.build();
    }

    @Override
    public void terminate() {
        // Not applicable
        throw new UnsupportedOperationException("Not Applicable. This implementation uses an " +
                "OkHttpClient instance, which reuse it for all of your HTTP calls.");
    }

    @Override
    public void cancel() {
        if (call != null) {
            call.cancel();
        }
    }

}
