/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.net.Uri;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.forgerock.android.auth.interceptor.AdviceHandler;
import org.forgerock.android.auth.interceptor.IdentityGatewayAdviceInterceptor;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class PolicyAdviceTest extends BaseTest {

    @Test
    public void testParse() {
        String source = "<Advices>" +
                "<AttributeValuePair>" +
                "<Attribute name=\"dummyName\"/>" +
                "<Value>dummyValue</Value>" +
                "</AttributeValuePair>" +
                "</Advices>";

        PolicyAdvice advice = PolicyAdvice.parse(source);
        assertThat(advice.toString()).isEqualTo(source);
    }

    @Test
    public void testTransactionConditionAdvice() throws InterruptedException, ExecutionException, IOException {

        String redirect = "http://openam.example.com:8081/openam/?goto=http://openig.example.com:8080/products?_txid%3D3b8c1b2b-0aed-461a-a49b-f35da8276d12&realm=/&authIndexType=composite_advice&authIndexValue=%3CAdvices%3E%3CAttributeValuePair%3E%3CAttribute%20name%3D%22TransactionConditionAdvice%22/%3E%3CValue%3E3b8c1b2b-0aed-461a-a49b-f35da8276d12%3C/Value%3E%3C/AttributeValuePair%3E%3C/Advices%3E";
        server.enqueue(new MockResponse()
                .addHeader("Location", redirect)
                .setResponseCode(307));
        enqueue("/products.json", HttpURLConnection.HTTP_OK);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(false);

        builder.addInterceptor(new IdentityGatewayAdviceInterceptor() {
            @Override
            public AdviceHandler getAdviceHandler(PolicyAdvice advice) {
                return (context, advice1) -> {
                    CompletableFuture<Void> future = new CompletableFuture();
                    future.complete(null);
                    assertThat(advice1.toString()).isEqualTo("<Advices><AttributeValuePair><Attribute name=\"TransactionConditionAdvice\"/><Value>3b8c1b2b-0aed-461a-a49b-f35da8276d12</Value></AttributeValuePair></Advices>");
                    return future;
                };
            }
        });

        OkHttpClient client = builder.build();
        Request request = new Request.Builder().url(getUrl() + "/products").build();
        FRListenerFuture<Response> future = new FRListenerFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.onException(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                future.onSuccess(response);
            }
        });

        Response result = future.get();
        assertThat(result.body().string()).isEqualTo("{\n" +
                "  \"products\": \"Android\"\n" +
                "}");

        server.takeRequest(); //Redirect
        RecordedRequest recordedRequest = server.takeRequest(); //resource

        assertThat(Uri.parse(recordedRequest.getPath()).getQueryParameter("_txid"))
                .isEqualTo("3b8c1b2b-0aed-461a-a49b-f35da8276d12");
    }

    @Test
    public void testAuthenticationToServiceConditionAdvice() throws InterruptedException, ExecutionException, IOException {

        String redirect = "http://openam.example.com:8081/openam/?goto=http://openig.example.com:8080/products&realm=/&authIndexType=composite_advice&authIndexValue=%3CAdvices%3E%3CAttributeValuePair%3E%3CAttribute%20name%3D%22AuthenticateToServiceConditionAdvice%22/%3E%3CValue%3E/:Example%3C/Value%3E%3C/AttributeValuePair%3E%3C/Advices%3E";
        server.enqueue(new MockResponse()
                .addHeader("Location", redirect)
                .setResponseCode(307));
        enqueue("/products.json", HttpURLConnection.HTTP_OK);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(false);

        builder.addInterceptor(new IdentityGatewayAdviceInterceptor() {
            @Override
            public AdviceHandler getAdviceHandler(PolicyAdvice advice) {
                return (context, advice1) -> {
                    CompletableFuture<Void> future = new CompletableFuture();
                    future.complete(null);
                    assertThat(advice1.toString()).isEqualTo("<Advices><AttributeValuePair><Attribute name=\"AuthenticateToServiceConditionAdvice\"/><Value>/:Example</Value></AttributeValuePair></Advices>");
                    return future;
                };
            }
        });

        OkHttpClient client = builder.build();
        Request request = new Request.Builder().url(getUrl() + "/products").build();
        FRListenerFuture<Response> future = new FRListenerFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.onException(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                future.onSuccess(response);
            }
        });

        Response result = future.get();
        assertThat(result.body().string()).isEqualTo("{\n" +
                "  \"products\": \"Android\"\n" +
                "}");

        server.takeRequest(); //Redirect
        server.takeRequest(); //resource

    }

    @Test
    public void testNoAdvice() throws InterruptedException, ExecutionException, IOException {

        String redirect = "http://openam.example.com:8081/openam/?goto=http://openig.example.com:8080/products&realm=/";
        server.enqueue(new MockResponse()
                .addHeader("Location", redirect)
                .setResponseCode(307));

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(false);

        builder.addInterceptor(new IdentityGatewayAdviceInterceptor() {
            @Override
            public AdviceHandler getAdviceHandler(PolicyAdvice advice) {
                return null;
            }
        });

        OkHttpClient client = builder.build();
        Request request = new Request.Builder().url(getUrl() + "/products").build();
        FRListenerFuture<Response> future = new FRListenerFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.onException(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                future.onSuccess(response);
            }
        });

        Response result = future.get();

        //When no advice return the original response.
        assertThat(result.code()).isEqualTo(307);
        assertThat(result.header("location")).isEqualTo(redirect);

    }

    @Test
    public void testFailedToParseAdvice() throws InterruptedException, ExecutionException, IOException {

        //Invalid advice xml format
        String redirect = "http://openam.example.com:8081/openam/?goto=http://openig.example.com:8080/products&realm=/&authIndexType=composite_advice&authIndexValue=<Advices><AttributeValuePair><Attribute name=\"AuthenticateToServiceConditionAdvice\"/><Value>/:Example</Value></AttributeValuePair>";
        server.enqueue(new MockResponse()
                .addHeader("Location", redirect)
                .setResponseCode(307));

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(false);

        builder.addInterceptor(new IdentityGatewayAdviceInterceptor() {
            @Override
            public AdviceHandler getAdviceHandler(PolicyAdvice advice) {
                return null;
            }
        });

        OkHttpClient client = builder.build();
        Request request = new Request.Builder().url(getUrl() + "/products").build();
        FRListenerFuture<Response> future = new FRListenerFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.onException(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                future.onSuccess(response);
            }
        });

        Response result = future.get();

        //When no advice return the original response.
        assertThat(result.code()).isEqualTo(307);
        assertThat(result.header("location")).isEqualTo(redirect);

    }
}
