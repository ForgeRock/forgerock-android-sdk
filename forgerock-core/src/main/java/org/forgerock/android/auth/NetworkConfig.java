/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import okhttp3.CookieJar;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Manages Network configuration information
 */
@Getter
class NetworkConfig {

    private String identifier;

    private String host;

    private Integer timeout;

    private TimeUnit timeUnit;

    private List<String> pins;

    private Supplier<CookieJar> cookieJarSupplier;

    private Supplier<List<Interceptor>> interceptorSupplier;

    private List<BuildStep<OkHttpClient.Builder>> buildSteps;

    @Builder(builderMethodName = "networkBuilder")
    NetworkConfig(String identifier,
                  @NonNull String host,
                  Integer timeout,
                  TimeUnit timeUnit,
                  Supplier<CookieJar> cookieJarSupplier,
                  @Singular List<String> pins,
                  Supplier<List<Interceptor>> interceptorSupplier,
                  @Singular List<BuildStep<OkHttpClient.Builder>> buildSteps) {

        this.identifier = identifier;
        this.host = host;
        this.timeout = timeout == null ? 30 : timeout;
        this.timeUnit = timeUnit == null ? SECONDS : timeUnit;
        this.pins = pins;
        this.cookieJarSupplier = cookieJarSupplier;
        this.interceptorSupplier = interceptorSupplier;
        this.buildSteps = buildSteps;
    }

    CookieJar getCookieJar() {
        if (cookieJarSupplier != null) {
            return cookieJarSupplier.get();
        } else {
            return CookieJar.NO_COOKIES;
        }
    }

    /**
     * Unique identifier for the Network config, the identifier will be used as the key to cache the
     * {@link okhttp3.OkHttpClient} inside {@link OkHttpClientProvider}
     *
     * @return The unique identifier to represent this configuration.
     */
    String getIdentifier() {
        if (identifier == null) {
            return getHost();
        } else {
            return identifier;
        }
    }
}
