/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.auth;

import androidx.annotation.NonNull;

import org.forgerock.android.auth.Action;
import org.forgerock.android.auth.Request;
import org.forgerock.android.auth.RequestActionInterceptor;
import org.forgerock.android.auth.RequestInterceptor;

/**
 * Sample {@link RequestActionInterceptor} to add header
 */
public class InjectHeaderAuthRequestInterceptor implements RequestActionInterceptor {

    @NonNull
    @Override
    public Request intercept(@NonNull Request request, Action action) {
        return request.newBuilder()
                .addHeader("headerName", "headerValue")
                .addHeader("headerName", "headerValue2")
                .build();

    }
}
