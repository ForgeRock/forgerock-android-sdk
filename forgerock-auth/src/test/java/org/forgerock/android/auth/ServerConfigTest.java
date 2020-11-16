/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RunWith(RobolectricTestRunner.class)
public class ServerConfigTest {

    public Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        OkHttpClientProvider.getInstance().clear();
    }

    @Test(expected = NullPointerException.class)
    public void testMissingContext() {
        ServerConfig.builder().build();
    }

    @Test
    public void testCachedOkHttpClient() {
        Config.getInstance().init(context);
        ServerConfig serverConfig = Config.getInstance().getServerConfig();
        OkHttpClient client1 = OkHttpClientProvider.getInstance().lookup(serverConfig);
        OkHttpClient client2 = OkHttpClientProvider.getInstance().lookup(serverConfig);
        Assertions.assertThat(client1 == client2).isTrue();
    }

    @Test
    public void testOkHttpCachedWithDifferentIdentifier() {
        Config.getInstance().init(context);
        ServerConfig serverConfig = Config.getInstance().getServerConfig();
        OkHttpClient client1 = OkHttpClientProvider.getInstance().lookup(serverConfig);
        Config.reset();
        Config.getInstance().init(context);
        ServerConfig serverConfig2 = Config.getInstance().getServerConfig();
        OkHttpClient client2 = OkHttpClientProvider.getInstance().lookup(serverConfig2);
        Assertions.assertThat(client1 != client2).isTrue();
    }


    /**
     * Generate PEM file
     * > sudo echo -n | openssl s_client -connect api.ipify.org:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ./api.ipify.org.pem
     * Generate Hash
     *
     * >openssl x509 -in api.ipify.org.pem -pubkey -noout | openssl rsa -pubin -outform der | openssl dgst -sha256 -binary | base64
     */
    @Test
    public void testSha256Pinning() throws InterruptedException {
        ServerConfig serverConfig = ServerConfig.builder()
                .context(context)
                .url("https://api.ipify.org")
                .pin("sha256/gAZLWmiY0ORGxqG0ccEhqiB3baugOOs9vdcezRCHc44=")
                .build();

        OkHttpClient client = OkHttpClientProvider.getInstance().lookup(serverConfig);

        Request request = new Request.Builder()
                .get()
                .url("https://api.ipify.org?format=json")
                .build();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        final boolean[] result = new boolean[1];

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                result[0] = false;
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                result[0] = true;
                countDownLatch.countDown();
            }

        });
        countDownLatch.await();
        Assert.assertTrue(result[0]);

    }

    @Test
    public void testSha1Pinning() throws InterruptedException {
        ServerConfig serverConfig = ServerConfig.builder()
                .context(context)
                .url("https://api.ipify.org")
                .pin("sha1/jgXAj7NCdI7mOsNIRIghvGKLgVA=")
                .build();

        OkHttpClient client = OkHttpClientProvider.getInstance().lookup(serverConfig);

        Request request = new Request.Builder()
                .get()
                .url("https://api.ipify.org?format=json")
                .build();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        final boolean[] result = new boolean[1];

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                result[0] = false;
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                result[0] = true;
                countDownLatch.countDown();
            }

        });
        countDownLatch.await();
        Assert.assertTrue(result[0]);

    }

    /**
     * A certificate chain is then valid only if the certificate chain contains at least one of the pinned public keys.
     */
    @Test
    public void testMultiplePinning() throws InterruptedException {
        ServerConfig serverConfig = ServerConfig.builder()
                .context(context)
                .url("https://api.ipify.org")
                .pin("sha256/gAZLWmiY0ORGxqG0ccEhqiB3baugOOs9vdcezRCHc44=")
                .pin("sha256/invalid")
                .build();

        OkHttpClient client = OkHttpClientProvider.getInstance().lookup(serverConfig);

        Request request = new Request.Builder()
                .get()
                .url("https://api.ipify.org?format=json")
                .build();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        final boolean[] result = new boolean[1];

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                result[0] = false;
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                result[0] = true;
                countDownLatch.countDown();
            }

        });
        countDownLatch.await();
        Assert.assertTrue(result[0]);

    }

    @Test
    public void testInvalidPin() throws InterruptedException {
        ServerConfig serverConfig = ServerConfig.builder()
                .context(context)
                .url("https://api.ipify.org")
                .pin("sha256/invalid=")
                .build();

        OkHttpClient client = OkHttpClientProvider.getInstance().lookup(serverConfig);

        Request request = new Request.Builder()
                .get()
                .url("https://api.ipify.org?format=json")
                .build();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        final boolean[] result = new boolean[1];
        result[0] = true;

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (e.getMessage().contains("Certificate pinning failure!")) {
                    result[0] = false;
                }
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                result[0] = true;
                countDownLatch.countDown();
            }

        });
        countDownLatch.await();
        Assert.assertFalse(result[0]);

    }
}