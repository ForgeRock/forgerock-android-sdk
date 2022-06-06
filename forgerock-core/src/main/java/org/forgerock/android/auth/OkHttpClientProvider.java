/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.CertificatePinner;
import okhttp3.CookieJar;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Provider to Cache and provide OKHttpClient
 */
class OkHttpClientProvider {

    private static final OkHttpClientProvider INSTANCE = new OkHttpClientProvider();

    private Map<String, OkHttpClient> cache = new ConcurrentHashMap<>();

    private final InterceptorProvider interceptorProvider = new InterceptorProvider();

    private OkHttpClientProvider() {
        CoreEventDispatcher.CLEAR_OKHTTP.addObserver((o, arg) -> clear());
    }

    public static OkHttpClientProvider getInstance() {
        return INSTANCE;
    }

    /**
     * Create or lookup a cached OKHttpClient
     *
     * @param networkConfig The Server configuration
     * @return The OkHttpClient
     */
    OkHttpClient lookup(NetworkConfig networkConfig) {
        OkHttpClient client = cache.get(networkConfig.getIdentifier());

        if (client != null) {
            return client;
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(networkConfig.getTimeout(), networkConfig.getTimeUnit())
                .readTimeout(networkConfig.getTimeout(), networkConfig.getTimeUnit())
                .writeTimeout(networkConfig.getTimeout(), networkConfig.getTimeUnit())
                .followRedirects(false);

        if (networkConfig.getCookieJar() == null) {
            builder.cookieJar(CookieJar.NO_COOKIES);
        } else {
            builder.cookieJar(networkConfig.getCookieJar());
        }

        if (networkConfig.getInterceptorSupplier() != null &&
                networkConfig.getInterceptorSupplier().get() != null) {
            for (Interceptor i : networkConfig.getInterceptorSupplier().get()) {
                builder.addInterceptor(i);
            }
        }

        HttpLoggingInterceptor networkInterceptor = interceptorProvider.getInterceptor();
        if (networkInterceptor != null) {
            builder.addInterceptor(networkInterceptor);
        }

        if (!networkConfig.getPins().isEmpty()) {
            CertificatePinner.Builder cpBuilder = new CertificatePinner.Builder();
            for (String s : networkConfig.getPins()) {
                cpBuilder.add(networkConfig.getHost(), "sha256/" + s);
            }
            builder.certificatePinner(cpBuilder.build());
        }

        for (BuildStep<OkHttpClient.Builder> buildStep : networkConfig.getBuildSteps()) {
            if (buildStep != null) {
                buildStep.build(builder);
            }
        }

        client = builder.build();
        cache.put(networkConfig.getIdentifier(), client);
        return client;

    }

    /**
     * Clear the cached {{@link OkHttpClient}}
     */
    public void clear() {
        cache.clear();
    }
}
