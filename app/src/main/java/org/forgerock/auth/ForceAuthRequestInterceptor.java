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
import org.forgerock.android.auth.RequestInterceptor;

import static org.forgerock.android.auth.Action.START_AUTHENTICATE;

/**
 * Sample {@link RequestInterceptor} to add ForceAuth
 */
public class ForceAuthRequestInterceptor implements RequestInterceptor {

    @NonNull
    @Override
    public Request intercept(@NonNull Request request) {
        if (((Action)request.tag()).getType().equals(START_AUTHENTICATE)) {
            return request.newBuilder()
                    .url(request.url().toString() + "&ForceAuth=true")
                    .build();
        }
        return request;
    }
}
