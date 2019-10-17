/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import lombok.RequiredArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Interceptor to inject access token to the API Request
 */
@RequiredArgsConstructor
class AccessTokenInterceptor implements Interceptor {

    private final String TAG = AccessTokenInterceptor.class.getSimpleName();
    private final SessionManager sessionManager;

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        AccessToken accessToken;
            try {
                accessToken = sessionManager.getAccessToken();
                return chain.proceed(chain.request().newBuilder()
                        .header("Authorization", "Bearer " + accessToken.getValue())
                        .build());

            } catch (AuthenticationRequiredException e) {
                Logger.warn(TAG, e, "Failed to inject a valid access token");
           }
        return chain.proceed(chain.request());
    }





}
