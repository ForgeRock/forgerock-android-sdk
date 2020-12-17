/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.interceptor;

import lombok.RequiredArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.Response;

import org.forgerock.android.auth.AccessToken;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Interceptor to inject access token to the API Request
 */
@RequiredArgsConstructor
public class AccessTokenInterceptor implements Interceptor {

    private static final String TAG = AccessTokenInterceptor.class.getSimpleName();

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        AccessToken accessToken;
            try {
                if (FRUser.getCurrentUser() != null) {
                    accessToken = FRUser.getCurrentUser().getAccessToken();
                    return chain.proceed(chain.request().newBuilder()
                            .header("Authorization", "Bearer " + accessToken.getValue())
                            .build());
                }
            } catch (Exception e) {
                Logger.warn(TAG, e, "Failed to inject a valid access token");
           }
        return chain.proceed(chain.request());
    }
}
