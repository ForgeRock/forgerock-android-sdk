/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import okhttp3.Response;

import org.forgerock.android.auth.exception.ApiException;
import org.forgerock.android.auth.exception.AuthenticationException;

import java.net.HttpURLConnection;

interface ResponseHandler {

    /**
     * Handle Exception, provide feedback to registered {@link FRListener}
     *
     * @param response API Response
     */
    default void handleError(Response response, FRListener<?> listener) {
        switch (response.code()) {
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                Listener.onException(listener, new AuthenticationException(response.code(), response.message(), getBody(response)));
                break;
            default:
                Listener.onException(listener, new ApiException(response.code(), response.message(), getBody(response)));
        }
    }


    default String getBody(Response response) {
        try {
            return response.body().string();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Close the response body stream.
     *
     * @param response API Response
     */
    default void close(Response response) {
        try {
            response.close();
        } catch (Exception e) {
            //ignore
        }

    }
}
