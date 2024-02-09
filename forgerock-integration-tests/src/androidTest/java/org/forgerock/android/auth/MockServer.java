/*
 * Copyright (c) 2021 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Mock of AM Server
 */
public abstract class MockServer {

    protected MockWebServer server;

    public Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void startServer() throws Exception {

        Logger.set(Logger.Level.DEBUG);

        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        server = new MockWebServer();
        server.start();

        Config.getInstance().init(context, null);
        Config.getInstance().setUrl(getUrl());
    }

    @After
    public void shutdown() {
        try {
            server.shutdown();
        } catch (IOException e) {
            Logger.warn(MockServer.class.getName(), "Failed to shutdown server", e);
        } finally {
            RequestInterceptorRegistry.getInstance().register(null);
            Config.getInstance().getTokenManager().clear();
            Config.getInstance().getSingleSignOnManager().clear();
            Config.reset();
        }
    }

    protected String getUrl() {
        return "http://" + server.getHostName() + ":" + server.getPort();
    }


    protected String getJson(String path) {
        try {
            return IOUtils.toString(getClass().getResourceAsStream(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void enqueue(String path, int statusCode) {
        server.enqueue(new MockResponse()
                .setResponseCode(statusCode)
                .addHeader("Content-Type", "application/json")
                .setBody(getJson(path)));
    }

}
