/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

/**
 * Observes, modifies outgoing request from the SDK.
 * Interceptors can be used to add, remove, or transform headers, url, method on the request.
 */
public interface FRRequestInterceptor<T> extends RequestInterceptor {

    default @NonNull
    Request intercept(@NonNull Request request) {
        return intercept(request, (T) request.tag());
    }

    /**
     * Intercepts outgoing request from the SDK.
     *
     * @param request The original outgoing request
     * @param tag  The tag associate with the request. The SDK Tag outbound request with {@link Action}
     * @return The Updated Request
     */

    @NonNull
    Request intercept(@NonNull Request request, T tag);

}
