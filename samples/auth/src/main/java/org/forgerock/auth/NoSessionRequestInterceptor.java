/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.auth;

import android.net.Uri;

import androidx.annotation.NonNull;

import org.forgerock.android.auth.Action;
import org.forgerock.android.auth.FRRequestInterceptor;
import org.forgerock.android.auth.Request;
import org.forgerock.android.auth.RequestInterceptor;

import static org.forgerock.android.auth.Action.AUTHENTICATE;

/**
 * Sample {@link RequestInterceptor} to add noSession
 */
public class NoSessionRequestInterceptor implements FRRequestInterceptor<Action> {

    @NonNull
    @Override
    public Request intercept(@NonNull Request request, Action tag) {
        if (tag.getType().equals(AUTHENTICATE)) {
            return request.newBuilder()
                    .url(Uri.parse(request.url().toString())
                            .buildUpon()
                            .appendQueryParameter("noSession", "true").toString())
                    .build();
        }
        return request;
    }
}
