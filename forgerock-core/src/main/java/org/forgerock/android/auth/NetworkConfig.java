/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import org.forgerock.android.core.R;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import okhttp3.CookieJar;
import okhttp3.Interceptor;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Manages Network configuration information
 */
@EqualsAndHashCode
@Getter
class NetworkConfig {

    private String url;

    private String realm;

    private Integer timeout;

    private TimeUnit timeUnit;

    private List<String> pins;

    private String host;

    private Supplier<CookieJar> cookieJarSupplier;

    private List<Interceptor> interceptors;

    @Builder(builderMethodName = "networkBuilder")
    public NetworkConfig(@NonNull Context context,
                         @NonNull String url,
                         String realm,
                         Integer timeout,
                         TimeUnit timeUnit,
                         Supplier<CookieJar> cookieJarSupplier,
                         @Singular List<String> pins,
                         @Singular List<Interceptor> interceptors) {

        this.url = url;
        try {
            this.host = new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        this.realm = realm == null ? context.getResources().getString(R.string.forgerock_realm) : realm;
        this.timeout = timeout == null ? context.getResources().getInteger(R.integer.forgerock_timeout) : timeout;
        this.timeUnit = timeUnit == null ? SECONDS : timeUnit;
        this.pins = pins == null ? Arrays.asList(context.getResources().getStringArray(R.array.forgerock_pins)) : pins;
        this.cookieJarSupplier = cookieJarSupplier;
        this.interceptors = interceptors;
    }

    CookieJar getCookieJar() {
        if (cookieJarSupplier != null) {
            return cookieJarSupplier.get();
        } else {
            return CookieJar.NO_COOKIES;
        }
    }
}
