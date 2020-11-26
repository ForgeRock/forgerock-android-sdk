/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
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
    private static final Action USER_INFO = new Action(Action.USER_INFO);

    @Builder
    private UserService(ServerConfig serverConfig) {
        client = OkHttpClientProvider.getInstance().lookup(serverConfig).newBuilder()
                .addInterceptor(new AccessTokenInterceptor())
                .build();

        this.serverConfig = serverConfig;

    }

    void userinfo(final FRListener<UserInfo> listener) {
        Request request = null;
        try {
            request = new Request.Builder()
                    .url(getUserInfoUrl())
                    .get()
                    .tag(USER_INFO)
                    .build();
        } catch (MalformedURLException e) {
            Listener.onException(listener, e);
            return;
        }

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                listener.onException(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
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

    private URL getUserInfoUrl() throws MalformedURLException {

        Uri.Builder builder = Uri.parse(serverConfig.getUrl()).buildUpon();
        if (StringUtils.isNotEmpty(serverConfig.getUserInfoEndpoint())) {
            builder.appendEncodedPath(serverConfig.getUserInfoEndpoint());
        } else {
            builder.appendPath("oauth2")
                    .appendPath("realms")
                    .appendPath(serverConfig.getRealm())
                    .appendPath("userinfo");
        }
        return new URL(builder.build().toString());
    }

}
