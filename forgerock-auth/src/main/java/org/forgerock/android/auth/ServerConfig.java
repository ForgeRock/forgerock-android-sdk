/*
 * Copyright (c) 2020 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static java.util.Collections.singletonList;

import android.content.Context;

import androidx.annotation.NonNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Singular;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;

/**
 * Manages Server configuration information
 */
public class ServerConfig extends NetworkConfig {

    public static final String API_VERSION_2_1 = "resource=2.1, protocol=1.0";
    public static final String API_VERSION_3_1 = "resource=3.1, protocol=1.0";
    public static final String API_VERSION_1_0 = "resource=1.0";
    public static final String ACCEPT_API_VERSION = "Accept-API-Version";

    private String url;
    private String realm;
    private String cookieName;

    /**
     * Server Endpoint setting, leave it empty to use default setting.
     */
    private String authenticateEndpoint;
    private String authorizeEndpoint;
    private String tokenEndpoint;
    private String revokeEndpoint;
    private String userInfoEndpoint;
    private String sessionEndpoint;
    private String endSessionEndpoint;

    @lombok.Builder
    private ServerConfig(@NonNull Context context,
                         String identifier,
                         String url,
                         String realm,
                         Integer timeout,
                         TimeUnit timeUnit,
                         Supplier<CookieJar> cookieJarSupplier,
                         @Singular List<String> pins,
                         @Singular List<BuildStep<OkHttpClient.Builder>> buildSteps,
                         String cookieName,
                         String authenticateEndpoint,
                         String authorizeEndpoint,
                         String tokenEndpoint,
                         String revokeEndpoint,
                         String userInfoEndpoint,
                         String sessionEndpoint,
                         String endSessionEndpoint) {
        super(identifier,
                getHost(context, url),
                getTimeOut(context, timeout),
                timeUnit, cookieJarSupplier,
                getPins(context, pins),
                () -> singletonList(new OkHttpRequestInterceptor()),
                buildSteps);
        this.url = url;
        this.realm = realm == null ? context.getResources().getString(R.string.forgerock_realm) : realm;
        this.cookieName = cookieName;
        this.authenticateEndpoint = trimLeadingSlash(authenticateEndpoint);
        this.authorizeEndpoint = trimLeadingSlash(authorizeEndpoint);
        this.tokenEndpoint = trimLeadingSlash(tokenEndpoint);
        this.revokeEndpoint = trimLeadingSlash(revokeEndpoint);
        this.userInfoEndpoint = trimLeadingSlash(userInfoEndpoint);
        this.sessionEndpoint = trimLeadingSlash(sessionEndpoint);
        this.endSessionEndpoint = trimLeadingSlash(endSessionEndpoint);

    }

    private String trimLeadingSlash(final String str) {
        return str == null ? str : str.replaceAll("^/+", "");
    }


    private static String getHost(Context context, String url) {
        try {
            String u = url == null ? context.getResources().getString(R.string.forgerock_url) : url;
            return new URL(u).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Integer getTimeOut(Context context, Integer timeout) {
        return timeout == null ? context.getResources().getInteger(R.integer.forgerock_timeout) : timeout;
    }

    private static List<String> getPins(Context context, List<String> pins) {
        return pins == null ? Arrays.asList(context.getResources().getStringArray(R.array.forgerock_ssl_pinning_public_key_hashes)) : pins;
    }

    public String getUrl() {
        return this.url;
    }

    public String getRealm() {
        return this.realm;
    }

    public String getCookieName() {
        return this.cookieName;
    }

    public String getAuthenticateEndpoint() {
        return this.authenticateEndpoint;
    }

    public String getAuthorizeEndpoint() {
        return this.authorizeEndpoint;
    }

    public String getTokenEndpoint() {
        return this.tokenEndpoint;
    }

    public String getRevokeEndpoint() {
        return this.revokeEndpoint;
    }

    public String getUserInfoEndpoint() {
        return this.userInfoEndpoint;
    }

    public String getSessionEndpoint() {
        return this.sessionEndpoint;
    }

    public String getEndSessionEndpoint() {
        return this.endSessionEndpoint;
    }
}
