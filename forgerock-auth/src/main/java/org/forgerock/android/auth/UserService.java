/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.net.Uri;
import lombok.Builder;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.forgerock.android.auth.interceptor.AccessTokenInterceptor;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * Service Client
 */
class UserService implements ResponseHandler {

    private OkHttpClient client;
    private ServerConfig serverConfig;

    @Builder
    public UserService(ServerConfig serverConfig, SessionManager sessionManager) {
        client = OkHttpClientProvider.getInstance().lookup(serverConfig).newBuilder()
                .addInterceptor(new AccessTokenInterceptor())
                .build();

        this.serverConfig = serverConfig;

    }

    void userinfo(final FRListener<UserInfo> listener) {
        Request request = new Request.Builder()
                .url(getUserInfoUrl())
                .get()
                .build();


        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                listener.onException(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(Objects.requireNonNull(response.body()).string());
                        Listener.onSuccess(listener, UserInfo.unmarshal(jsonObject));
                    } catch (Exception e) {
                        Listener.onException(listener, e);
                    }
                } else {
                    handleError(response, listener);
                }
            }
        });
    }

    private URL getUserInfoUrl() {
        try {
            return new URL(Uri.parse(serverConfig.getUrl())
                    .buildUpon()
                    .appendPath("oauth2")
                    .appendPath("realms")
                    .appendPath(serverConfig.getRealm())
                    .appendPath("userinfo")
                    .build().toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
