/*
 * Copyright (c) 2020 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static java.util.concurrent.TimeUnit.SECONDS;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Singular;
import okhttp3.CookieJar;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

/**
 * Manages Network configuration information
 */
public class NetworkConfig {

    private String identifier;

    private String host;

    private Integer timeout;

    private TimeUnit timeUnit;

    private List<String> pins;

    private Supplier<CookieJar> cookieJarSupplier;

    private Supplier<List<Interceptor>> interceptorSupplier;

    private List<BuildStep<OkHttpClient.Builder>> buildSteps;

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

    public static NetworkConfigBuilder networkBuilder() {
        return new NetworkConfigBuilder();
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

    public String getHost() {
        return this.host;
    }

    public Integer getTimeout() {
        return this.timeout;
    }

    public TimeUnit getTimeUnit() {
        return this.timeUnit;
    }

    public List<String> getPins() {
        return this.pins;
    }

    public Supplier<CookieJar> getCookieJarSupplier() {
        return this.cookieJarSupplier;
    }

    public Supplier<List<Interceptor>> getInterceptorSupplier() {
        return this.interceptorSupplier;
    }

    public List<BuildStep<OkHttpClient.Builder>> getBuildSteps() {
        return this.buildSteps;
    }

    public static class NetworkConfigBuilder {
        private String identifier;
        private String host;
        private Integer timeout;
        private TimeUnit timeUnit;
        private Supplier<CookieJar> cookieJarSupplier;
        private ArrayList<String> pins;
        private Supplier<List<Interceptor>> interceptorSupplier;
        private ArrayList<BuildStep<OkHttpClient.Builder>> buildSteps;

        NetworkConfigBuilder() {
        }

        public NetworkConfigBuilder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public NetworkConfigBuilder host(String host) {
            this.host = host;
            return this;
        }

        public NetworkConfigBuilder timeout(Integer timeout) {
            this.timeout = timeout;
            return this;
        }

        public NetworkConfigBuilder timeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        public NetworkConfigBuilder cookieJarSupplier(Supplier<CookieJar> cookieJarSupplier) {
            this.cookieJarSupplier = cookieJarSupplier;
            return this;
        }

        public NetworkConfigBuilder pin(String pin) {
            if (this.pins == null) this.pins = new ArrayList<String>();
            this.pins.add(pin);
            return this;
        }

        public NetworkConfigBuilder pins(Collection<? extends String> pins) {
            if (pins == null) {
                throw new NullPointerException("pins cannot be null");
            }
            if (this.pins == null) this.pins = new ArrayList<String>();
            this.pins.addAll(pins);
            return this;
        }

        public NetworkConfigBuilder clearPins() {
            if (this.pins != null)
                this.pins.clear();
            return this;
        }

        public NetworkConfigBuilder interceptorSupplier(Supplier<List<Interceptor>> interceptorSupplier) {
            this.interceptorSupplier = interceptorSupplier;
            return this;
        }

        public NetworkConfigBuilder buildStep(BuildStep<OkHttpClient.Builder> buildStep) {
            if (this.buildSteps == null)
                this.buildSteps = new ArrayList<>();
            this.buildSteps.add(buildStep);
            return this;
        }

        public NetworkConfigBuilder buildSteps(Collection<? extends BuildStep<OkHttpClient.Builder>> buildSteps) {
            if (buildSteps == null) {
                throw new NullPointerException("buildSteps cannot be null");
            }
            if (this.buildSteps == null)
                this.buildSteps = new ArrayList<BuildStep<OkHttpClient.Builder>>();
            this.buildSteps.addAll(buildSteps);
            return this;
        }

        public NetworkConfigBuilder clearBuildSteps() {
            if (this.buildSteps != null)
                this.buildSteps.clear();
            return this;
        }

        public NetworkConfig build() {
            List<String> pins;
            switch (this.pins == null ? 0 : this.pins.size()) {
                case 0:
                    pins = java.util.Collections.emptyList();
                    break;
                case 1:
                    pins = java.util.Collections.singletonList(this.pins.get(0));
                    break;
                default:
                    pins = List.copyOf(this.pins);
            }
            List<BuildStep<OkHttpClient.Builder>> buildSteps;
            switch (this.buildSteps == null ? 0 : this.buildSteps.size()) {
                case 0:
                    buildSteps = java.util.Collections.emptyList();
                    break;
                case 1:
                    buildSteps = java.util.Collections.singletonList(this.buildSteps.get(0));
                    break;
                default:
                    buildSteps = List.copyOf(this.buildSteps);
            }

            return new NetworkConfig(this.identifier, this.host, this.timeout, this.timeUnit, this.cookieJarSupplier, pins, this.interceptorSupplier, buildSteps);
        }

        public String toString() {
            return "NetworkConfig.NetworkConfigBuilder(identifier=" + this.identifier + ", host=" + this.host + ", timeout=" + this.timeout + ", timeUnit=" + this.timeUnit + ", cookieJarSupplier=" + this.cookieJarSupplier + ", pins=" + this.pins + ", interceptorSupplier=" + this.interceptorSupplier + ", buildSteps=" + this.buildSteps + ")";
        }
    }
}
