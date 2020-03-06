/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.CertificatePinner;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Provider to Cache and provide OKHttpClient
 */
class OkHttpClientProvider {

    private static final OkHttpClientProvider INSTANCE = new OkHttpClientProvider();

    private Map<ServerConfig, OkHttpClient> cache = new ConcurrentHashMap<>();

    private OkHttpClientProvider() {
    }

    public static OkHttpClientProvider getInstance() {
        return INSTANCE;
    }

    /**
     * Create or lookup a cached OKHttpClient
     *
     * @param serverConfig The Server configuration
     * @return The OkHttpClient
     */
    OkHttpClient lookup(ServerConfig serverConfig) {
        OkHttpClient client = cache.get(serverConfig);

        if (client != null) {
            return client;
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(serverConfig.getTimeout(), serverConfig.getTimeUnit())
                .readTimeout(serverConfig.getTimeout(), serverConfig.getTimeUnit())
                .writeTimeout(serverConfig.getTimeout(), serverConfig.getTimeUnit())
                .followRedirects(false);

        if(serverConfig.getCookieJar() == null){
            builder.cookieJar(CookieJar.NO_COOKIES);
        } else {
            builder.cookieJar(serverConfig.getCookieJar());
        }

        if (Logger.isDebugEnabled()) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.level(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(interceptor);
        }

        if (!serverConfig.getPins().isEmpty()) {
            CertificatePinner.Builder cpBuilder = new CertificatePinner.Builder();
            for (String s : serverConfig.getPins()) {
                cpBuilder.add(serverConfig.getHost(), s);
            }
            builder.certificatePinner(cpBuilder.build());
        }

        client = builder.build();
        cache.put(serverConfig, client);
        return client;

    }

    /**
     * Clear the cached {{@link OkHttpClient}}
     */
    public void clear() {
        cache.clear();
    }
}
