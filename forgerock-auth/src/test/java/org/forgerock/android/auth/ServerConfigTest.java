/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RunWith(AndroidJUnit4.class)
public class ServerConfigTest {

    public Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        OkHttpClientProvider.getInstance().clear();
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }

    @Test(expected = NullPointerException.class)
    public void testMissingContext() {
        ServerConfig.builder().build();
    }

    @Test
    public void testCachedOkHttpClient() {
        Config.getInstance().init(context, null);
        ServerConfig serverConfig = Config.getInstance().getServerConfig();
        OkHttpClient client1 = OkHttpClientProvider.getInstance().lookup(serverConfig);
        OkHttpClient client2 = OkHttpClientProvider.getInstance().lookup(serverConfig);
        assertThat(client1 == client2).isTrue();
    }

    @Test
    public void testOkHttpCachedWithDifferentIdentifier() {
        Config.getInstance().init(context, null);
        ServerConfig serverConfig = Config.getInstance().getServerConfig();
        OkHttpClient client1 = OkHttpClientProvider.getInstance().lookup(serverConfig);
        Config.reset();
        Config.getInstance().init(context, null);
        ServerConfig serverConfig2 = Config.getInstance().getServerConfig();
        OkHttpClient client2 = OkHttpClientProvider.getInstance().lookup(serverConfig2);
        assertThat(client1 != client2).isTrue();
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
                .pin("9hNxmEFgLKGJXqgp61hyb8yIyiT9u0vgDZh4y8TmY/M=")
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
        assertTrue(result[0]);

    }

    /**
     * A certificate chain is then valid only if the certificate chain contains at least one of the pinned public keys.
     */
    @Test
    public void testMultiplePinning() throws InterruptedException {
        ServerConfig serverConfig = ServerConfig.builder()
                .context(context)
                .url("https://api.ipify.org")
                .pin("9hNxmEFgLKGJXqgp61hyb8yIyiT9u0vgDZh4y8TmY/M=")
                .pin("invalid")
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
        assertTrue(result[0]);

    }

    @Test
    public void testInvalidPin() throws InterruptedException {
        ServerConfig serverConfig = ServerConfig.builder()
                .context(context)
                .url("https://api.ipify.org")
                .pin("invalid=")
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
        assertFalse(result[0]);
    }

    @Test
    public void testPinningConfig() {
        Config.getInstance().init(context, null);
        ServerConfig serverConfig = Config.getInstance().getServerConfig();
        OkHttpClient client = OkHttpClientProvider.getInstance().lookup(serverConfig);
        assertThat(client.certificatePinner().getPins())
                .contains(new CertificatePinner.Pin(serverConfig.getHost(),
                        "sha256/9hNxmEFgLKGJXqgp61hyb8yIyiT9u0vgDZh4y8TmY/M="));
    }

    @Test
    public void testBuildStepWithCustomPin() throws InterruptedException {
        ServerConfig serverConfig = ServerConfig.builder()
                .context(context)
                .url("https://api.ipify.org")
                .buildStep(builder -> builder.certificatePinner(
                        new CertificatePinner.Builder().add("api.ipify.org", "sha1/2vB3hhEJ98C5efhhWpxtD2wxYek=" ).build()))
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
        assertTrue(result[0]);

    }

    @Test
    public void testBuildStepWithCustomPinFailed() throws InterruptedException {
        ServerConfig serverConfig = ServerConfig.builder()
                .context(context)
                .url("https://api.ipify.org")
                .buildStep(builder -> builder.certificatePinner(
                        new CertificatePinner.Builder().add("api.ipify.org", "sha256/invalid=" ).build()))
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
        assertFalse(result[0]);

    }

    @Test
    public void testCustomPath() {
        ServerConfig serverConfig = ServerConfig.builder()
                .context(context)
                .authenticateEndpoint("//////authenticate")
                .authorizeEndpoint("authorize")
                .tokenEndpoint("/token")
                .revokeEndpoint("//revoke/test")
                .userInfoEndpoint("//userInfo/test/")
                .sessionEndpoint("//session")
                .build();

        assertThat(serverConfig.getAuthenticateEndpoint()).isEqualTo("authenticate");
        assertThat(serverConfig.getAuthorizeEndpoint()).isEqualTo("authorize");
        assertThat(serverConfig.getTokenEndpoint()).isEqualTo("token");
        assertThat(serverConfig.getRevokeEndpoint()).isEqualTo("revoke/test");
        assertThat(serverConfig.getUserInfoEndpoint()).isEqualTo("userInfo/test/");
        assertThat(serverConfig.getSessionEndpoint()).isEqualTo("session");
        assertThat(serverConfig.getEndSessionEndpoint()).isNull();
    }
}