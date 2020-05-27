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
public interface RequestInterceptor {

    /**
     * Intercepts outgoing request from the SDK.
     *
     * @param request The original outgoing request
     * @return The Updated Request
     */
    @NonNull
    Request intercept(@NonNull Request request);

}
