/*
 * This is WestJet source code and is for consideration as a pull request to ForgeRock.
 *
 * This fork was necessary to integrate with the F5® Distributed Cloud Defense Mobile SDK,
 * which protects API endpoints from automation attacks by collecting telemetry and adding
 * custom HTTP headers to requests. The response handling capability was built into the
 * ForgeRock SDK to ensure that the F5 Distributed Cloud Bot Defense Mobile SDK can inspect
 * and process response headers for its internal functionality.
 *
 * Dated: 2024
 */

package org.forgerock.android.auth;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Interceptor to intercept Http Response and invoke registered {@link ResponseInterceptor}
 */
class OkHttpResponseInterceptor implements Interceptor {

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        // Wrap the okhttp3.Response with the custom Response class
        org.forgerock.android.auth.Response customResponse = new org.forgerock.android.auth.Response(response);

        ResponseInterceptor[] interceptors = ResponseInterceptorRegistry.getInstance().getResponseInterceptors();
        if (interceptors != null) {
            for (ResponseInterceptor i : interceptors) {
                customResponse = i.intercept(customResponse);
            }
        }

        return customResponse.getInternalRes();
    }
}