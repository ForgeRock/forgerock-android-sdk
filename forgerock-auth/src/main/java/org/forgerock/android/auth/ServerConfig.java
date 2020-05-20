/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import okhttp3.CookieJar;

/**
 * Manages Server configuration information
 */
@Getter
public class ServerConfig extends NetworkConfig {

    public static final String API_VERSION_2_1 = "resource=2.1, protocol=1.0";
    public static final String API_VERSION_3_1 = "resource=3.1, protocol=1.0";
    public static final String ACCEPT_API_VERSION = "Accept-API-Version";

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

        //TODO Inject Interceptor
        super(context, url, realm, timeout, timeUnit, cookieJarSupplier, pins, new ArrayList<>());

        this.authenticateEndpoint = authenticateEndpoint;
        this.authorizeEndpoint = authorizeEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.revokeEndpoint = revokeEndpoint;
        this.userInfoEndpoint = userInfoEndpoint;
        this.logoutEndpoint = logoutEndpoint;
    }

}
