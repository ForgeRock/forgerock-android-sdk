/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Interceptor to intercept Http Request, transform the {@link okhttp3.Request} to {@link Request}
 * and invoke registered {@link RequestInterceptor}
 */
class OkHttpRequestInterceptor implements Interceptor {

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        RequestInterceptor[] interceptors = RequestInterceptorRegistry.getInstance().getRequestInterceptors() ;
        if (interceptors == null || interceptors.length == 0) {
            //If no interceptors, continue the chain
            return chain.proceed(chain.request());
        }
        Request updatedRequest = new Request(chain.request());

        for (RequestInterceptor i : interceptors) {
            updatedRequest = i.intercept(updatedRequest);
        }
        return chain.proceed(updatedRequest.getInternalReq());
    }

}
