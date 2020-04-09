/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import lombok.*;
import okhttp3.CookieJar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Manages Server configuration information
 */
@EqualsAndHashCode
@Getter
public class ServerConfig {

    public static final String API_VERSION_1_0 = "resource=1.0, protocol=1.0";
    public static final String API_VERSION_2_1 = "resource=2.1, protocol=1.0";
    public static final String API_VERSION_3_1 = "resource=3.1, protocol=1.0";
    public static final String ACCEPT_API_VERSION = "Accept-API-Version";

    private String url;

    private String realm;

    private Integer timeout;

    private TimeUnit timeUnit;

    private List<String> pins;

    private String host;

    private Supplier<CookieJar> cookieJarSupplier;

    /**
     * Server Endpoint setting, leave it empty to use default setting.
     */
    private String authenticateEndpoint;
    private String authorizeEndpoint;
    private String tokenEndpoint;
    private String revokeEndpoint;
    private String userInfoEndpoint;
    private String logoutEndpoint;

    @lombok.Builder
    public ServerConfig(@NonNull Context context,
                        @NonNull String url,
                        String realm,
                        Integer timeout,
                        TimeUnit timeUnit,
                        Supplier<CookieJar> cookieJarSupplier,
                        @Singular List<String> pins,
                        String authenticateEndpoint,
                        String authorizeEndpoint,
                        String tokenEndpoint,
                        String revokeEndpoint,
                        String userInfoEndpoint,
                        String logoutEndpoint) {

        this.url = url;
        try {
            this.host = new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        this.realm = realm == null ? context.getResources().getString(R.string.forgerock_realm) : realm;
        this.timeout = timeout == null ? context.getResources().getInteger(R.integer.forgerock_timeout) : timeout;
        this.timeUnit = timeUnit == null ? SECONDS : timeUnit;
        this.pins = Arrays.asList(context.getResources().getStringArray(R.array.forgerock_pins));
        this.cookieJarSupplier = cookieJarSupplier;
        this.authenticateEndpoint = authenticateEndpoint;
        this.authorizeEndpoint = authorizeEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.revokeEndpoint = revokeEndpoint;
        this.userInfoEndpoint = userInfoEndpoint;
        this.logoutEndpoint = logoutEndpoint;
    }

    CookieJar getCookieJar() {
        if (cookieJarSupplier != null) {
            return cookieJarSupplier.get();
        } else {
            return CookieJar.NO_COOKIES;
        }
    }
}
