/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;

@RunWith(RobolectricTestRunner.class)
public class NetworkConfigTest {
    protected MockWebServer server;

    @Before
    public void setUp() throws IOException, JSONException {
        OkHttpClientProvider.getInstance().clear();
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void testBuildStepGotExecutedWithCustomTimeout() {
        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .timeout(1000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .buildStep(builder -> {
                    builder.writeTimeout(1111, TimeUnit.MILLISECONDS);
                })
                .buildStep(builder -> {
                    builder.readTimeout(2222, TimeUnit.MILLISECONDS);
                })
                .build();
        OkHttpClient client = OkHttpClientProvider.getInstance().lookup(networkConfig);
        //Timeout got overridden
        assertThat(client.writeTimeoutMillis()).isEqualTo(1111);
        assertThat(client.readTimeoutMillis()).isEqualTo(2222);
    }

    @Test
    public void testBuildStepWithNullInput() {
        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .timeout(1000)
                .timeUnit(TimeUnit.MILLISECONDS)
                .buildStep(null)
                .build();
        OkHttpClient client = OkHttpClientProvider.getInstance().lookup(networkConfig);
        //Timeout got overridden
        assertThat(client.writeTimeoutMillis()).isEqualTo(1000);
    }

    @Test
    public void testPinningConfig() {
        NetworkConfig networkConfig = NetworkConfig.networkBuilder()
                .host(server.getHostName())
                .pin("9hNxmEFgLKGJXqgp61hyb8yIyiT9u0vgDZh4y8TmY/M=")
                .build();
        OkHttpClient client = OkHttpClientProvider.getInstance().lookup(networkConfig);
        //Make sure the pinning is configured
        Assertions.assertThat(client.certificatePinner().getPins())
                .contains(new CertificatePinner.Pin(server.getHostName(),
                        "sha256/9hNxmEFgLKGJXqgp61hyb8yIyiT9u0vgDZh4y8TmY/M="));
    }


}
