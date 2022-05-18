/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static java.util.Collections.singletonList;

import android.net.Uri;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import kotlin.Pair;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

@RunWith(RobolectricTestRunner.class)
public class RequestInterceptorTest {
    protected MockWebServer server;
    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    private JSONObject data;

    @Before
    public void setUp() throws IOException, JSONException {
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        server = new MockWebServer();
        server.start();
        server.enqueue(new MockResponse().setResponseCode(200));
        data = new JSONObject();
        data.put("test", "test");
        OkHttpClientProvider.getInstance().clear();
        RequestInterceptorRegistry.getInstance().register(null);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        OkHttpClientProvider.getInstance().clear();
        RequestInterceptorRegistry.getInstance().register(null);
    }

    private String getUrl() {
        return "http://" + server.getHostName() + ":" + server.getPort();
    }

    @Test
    public void testSimpleIntercept() throws InterruptedException {
        final Boolean[] executed = {false};
        RequestInterceptorRegistry.getInstance().register(request -> {
            assertThat(request.url().toString()).isEqualTo(getUrl() + "/");
            assertThat(request.header("HeaderName")).isEqualTo("OriginalValue");
            assertThat(request.method()).isEqualTo("POST");
            assertThat(new String(request.body().getContent())).isEqualTo(data.toString());
            assertThat(request.headers("HeaderName")).contains("OriginalValue");
            Iterator<Pair<String, String>> iterator = request.headers();
            int count = 0;
            while (iterator.hasNext()) {
                count++;
                Pair<String, String> i = iterator.next();
                assertThat(i.component1()).isEqualTo("HeaderName");
                assertThat(i.component2()).isEqualTo("OriginalValue");
            }
            assertThat(count).isEqualTo(1);
            executed[0] = true;
            return request;
        });
        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .header("HeaderName", "OriginalValue")
                .post(RequestBody.create(data.toString(), JSON))
                .build();
        send(networkConfig, request);
        assertThat(executed[0]).isTrue();
    }

    @Test
    public void testChainIntercept() throws InterruptedException {

        RequestInterceptorRegistry.getInstance().register(
                request -> request.newBuilder().addHeader("HeaderName", "HeaderValue").build(),
                request -> request.newBuilder().addHeader("HeaderName2", "HeaderValue2").build());


        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .get()
                .build();
        send(networkConfig, request);
        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getHeader("HeaderName")).isEqualTo("HeaderValue");
        assertThat(recordedRequest.getHeader("HeaderName2")).isEqualTo("HeaderValue2");
    }

    @Test
    public void testAddHeader() throws InterruptedException {

        RequestInterceptorRegistry.getInstance().register(
                request -> request.newBuilder().addHeader("HeaderName", "HeaderValue").build());

        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .get()
                .build();
        send(networkConfig, request);
        assertThat(server.takeRequest().getHeader("HeaderName")).isEqualTo("HeaderValue");
    }

    @Test
    public void testRemoveHeader() throws InterruptedException {
        RequestInterceptorRegistry.getInstance().register(
                request -> request.newBuilder().removeHeader("HeaderName").build());

        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .header("HeaderName", "OriginalValue")
                .url(getUrl())
                .get()
                .build();
        send(networkConfig, request);
        assertThat(server.takeRequest().getHeader("HeaderName")).isNull();
    }

    @Test
    public void testReplaceHeader() throws InterruptedException {
        RequestInterceptorRegistry.getInstance().register(
                request -> request.newBuilder().header("HeaderName", "HeaderValue2").build());

        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .header("HeaderName", "HeaderValue")
                .get()
                .build();
        send(networkConfig, request);
        assertThat(server.takeRequest().getHeader("HeaderName")).isEqualTo("HeaderValue2");
    }

    @Test
    public void testCustomizeUrl() throws InterruptedException {
        RequestInterceptorRegistry.getInstance().register(
                request -> request.newBuilder().url(getUrl() + "/somewhere").build());

        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .get()
                .build();
        send(networkConfig, request);
        assertThat(server.takeRequest().getPath()).isEqualTo("/somewhere");

    }

    @Test
    public void testCustomizeParam() throws InterruptedException, JSONException {

        RequestInterceptorRegistry.getInstance().register(
                request -> request.newBuilder().url(getUrl() + "?forceAuth=true").build());

        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .get()
                .build();
        send(networkConfig, request);
        Uri uri = Uri.parse(server.takeRequest().getPath());
        assertThat(uri.getQueryParameter("forceAuth")).isEqualTo("true");

    }

    @Test
    public void testCustomizeBody() throws InterruptedException, JSONException {
        JSONObject sample = new JSONObject();
        sample.put("sampleName", "sampleValue");

        RequestInterceptorRegistry.getInstance().register(
                request -> {
                    //Make sure we able to retrieve the existing content
                    assertThat(new String(request.body().getContent())).isEqualTo(data.toString());
                    assertThat(request.body().getContentType()).isEqualTo(JSON.toString());
                    return request.newBuilder()
                            .post(new Body(sample.toString(), JSON.toString()))
                            .build();
                });

        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        RequestBody.create("", JSON);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .post(RequestBody.create(data.toString(), JSON))
                .build();
        send(networkConfig, request);
        JSONObject result = new JSONObject(server.takeRequest().getBody().readUtf8());
        assertThat(result.getString("sampleName")).isEqualTo("sampleValue");

    }

    @Test
    public void testGet() throws InterruptedException {

        RequestInterceptorRegistry.getInstance().register(
                request -> request.newBuilder().get().build());

        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        RequestBody.create("", JSON);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .post(RequestBody.create(data.toString(), JSON))
                .build();
        send(networkConfig, request);
        assertThat(server.takeRequest().getMethod()).isEqualTo("GET");

    }

    @Test
    public void testPut() throws InterruptedException, JSONException {
        JSONObject sample = new JSONObject();
        sample.put("sampleName", "sampleValue");

        RequestInterceptorRegistry.getInstance().register(
                request -> request.newBuilder().put(new Body(sample.toString(), JSON.toString())).build());

        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        RequestBody.create("", JSON);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .post(RequestBody.create(data.toString(), JSON))
                .build();
        send(networkConfig, request);
        RecordedRequest recordedRequest = server.takeRequest();
        JSONObject result = new JSONObject(recordedRequest.getBody().readUtf8());
        assertThat(result.getString("sampleName")).isEqualTo("sampleValue");
        assertThat(recordedRequest.getMethod()).isEqualTo("PUT");

    }

    @Test
    public void testPatch() throws JSONException, InterruptedException {

        JSONObject sample = new JSONObject();
        sample.put("sampleName", "sampleValue");

        RequestInterceptorRegistry.getInstance().register(
                request -> request.newBuilder().patch(new Body(sample.toString(), JSON.toString())).build());

        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        RequestBody.create("", JSON);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .post(RequestBody.create(data.toString(), JSON))
                .build();
        send(networkConfig, request);
        RecordedRequest recordedRequest = server.takeRequest();
        JSONObject result = new JSONObject(recordedRequest.getBody().readUtf8());
        assertThat(result.getString("sampleName")).isEqualTo("sampleValue");
        assertThat(recordedRequest.getMethod()).isEqualTo("PATCH");

    }

    @Test
    public void testDelete() throws JSONException, InterruptedException {
        JSONObject sample = new JSONObject();
        sample.put("sampleName", "sampleValue");

        RequestInterceptorRegistry.getInstance().register(
                request -> request.newBuilder().delete().build());

        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        RequestBody.create("", JSON);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .post(RequestBody.create(data.toString(), JSON))
                .build();
        send(networkConfig, request);
        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getBody().size()).isEqualTo(0);
        assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");
    }

    @Test
    public void testDeleteWithBody() throws JSONException, InterruptedException {
        JSONObject sample = new JSONObject();
        sample.put("sampleName", "sampleValue");

        RequestInterceptorRegistry.getInstance().register(
                request -> request.newBuilder().delete(new Body(sample.toString(), JSON.toString())).build());


        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        RequestBody.create("", JSON);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .post(RequestBody.create(data.toString(), JSON))
                .build();
        send(networkConfig, request);
        RecordedRequest recordedRequest = server.takeRequest();
        JSONObject result = new JSONObject(recordedRequest.getBody().readUtf8());
        assertThat(result.getString("sampleName")).isEqualTo("sampleValue");
        assertThat(recordedRequest.getMethod()).isEqualTo("DELETE");

    }

    @Test
    public void testActionTag() throws InterruptedException {

        RequestInterceptorRegistry.getInstance().register(
                request -> {
                    Action action = (Action) request.tag();
                    assertThat(action.getType()).isEqualTo("TEST");
                    return request;
                });


        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        RequestBody.create("", JSON);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .post(RequestBody.create(data.toString(), JSON))
                .tag(new Action("TEST"))
                .build();
        send(networkConfig, request);

    }

    @Test
    public void testRequestActionTag() throws InterruptedException {

        RequestInterceptorRegistry.getInstance().register(
                (FRRequestInterceptor<Action>) (request, action) -> {
                    assertThat(action.getType()).isEqualTo("TEST");
                    return request;
                });

        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .interceptorSupplier(() -> singletonList(new OkHttpRequestInterceptor())).build();
        RequestBody.create("", JSON);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getUrl())
                .post(RequestBody.create(data.toString(), JSON))
                .tag(new Action("TEST"))
                .build();

        send(networkConfig, request);

    }

    @Test
    public void testRegistry() {
        RequestInterceptorRegistry.getInstance().register(request -> request,
                (FRRequestInterceptor) (request, action) -> request);
        assertThat(RequestInterceptorRegistry.getInstance().getRequestInterceptors().length).isEqualTo(2);
    }

    private void send(NetworkConfig networkConfig, okhttp3.Request request) throws InterruptedException {
        OkHttpClient client = OkHttpClientProvider.getInstance().lookup(networkConfig);
        CountDownLatch countDownLatch = new CountDownLatch(1);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
    }


}
