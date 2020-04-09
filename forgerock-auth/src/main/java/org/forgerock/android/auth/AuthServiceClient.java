/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.net.Uri;

import okhttp3.*;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.forgerock.android.auth.ServerConfig.ACCEPT_API_VERSION;
import static org.forgerock.android.auth.ServerConfig.API_VERSION_2_1;
import static org.forgerock.android.auth.StringUtils.isNotEmpty;

/**
 * Client to interact with the auth tree APIs
 */
class AuthServiceClient {

    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    private static final String AUTH_INDEX_TYPE = "authIndexType";
    private static final String AUTH_INDEX_VALUE = "authIndexValue";

    private ServerConfig serverConfig;
    private OkHttpClient okHttpClient;

    AuthServiceClient(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        this.okHttpClient = OkHttpClientProvider.getInstance().lookup(serverConfig);
    }

    /**
     * Start authentication with the auth tree
     *
     * @param authService The AuthService
     * @param handler     The response handler to handle the API result.
     */
    void authenticate(final AuthService authService, final AuthServiceResponseHandler handler) {
        try {
            Request request = new Request.Builder()
                    .url(getUrl(authService))
                    .post(RequestBody.create(new byte[0]))
                    .header(ACCEPT_API_VERSION, API_VERSION_2_1)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    handler.handleError(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    handler.handleResponse(response);
                }

            });

        } catch (Exception e) {
            handler.handleError(e);
        }
    }

    /**
     * Go to next node from the auth tree
     *
     * @param node    The current node
     * @param handler The response handler to handle the API result.
     */
    void authenticate(final Node node, final AuthServiceResponseHandler handler) {
        try {
            Request request = new Request.Builder()
                    .url(getUrl())
                    .post(RequestBody.create(node.toJsonObject().toString(), JSON))
                    .header(ACCEPT_API_VERSION, ServerConfig.API_VERSION_2_1)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    handler.handleError(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    handler.handleResponse(response);
                }
            });

        } catch (Exception e) {
            handler.handleError(e);
        }
    }

    private URL getUrl(AuthService authService) throws MalformedURLException {
        return new URL(getUriBuilder()
                .appendQueryParameter(AUTH_INDEX_TYPE, authService.getAuthIndexType())
                .appendQueryParameter(AUTH_INDEX_VALUE, authService.getAuthIndexValue())
                .build().toString());
    }

    private URL getUrl() throws MalformedURLException {
        return new URL(getUriBuilder()
                .build().toString());
    }

    private Uri.Builder getUriBuilder() {

        Uri.Builder builder = Uri.parse(serverConfig.getUrl()).buildUpon();
        if (isNotEmpty(serverConfig.getAuthenticateEndpoint())) {
            builder.appendEncodedPath(serverConfig.getAuthenticateEndpoint());
        } else {
            builder.appendPath("json")
                    .appendPath("realms")
                    .appendPath(serverConfig.getRealm())
                    .appendPath("authenticate");
        }
        return builder;
    }
}
