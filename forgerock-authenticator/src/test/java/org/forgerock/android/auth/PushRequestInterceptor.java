/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.forgerock.android.auth.Action.PUSH_AUTHENTICATE;
import static org.forgerock.android.auth.Action.PUSH_REGISTER;

import android.net.Uri;

import androidx.annotation.NonNull;

public class PushRequestInterceptor implements FRRequestInterceptor<Action> {
    @NonNull
    @Override
    public Request intercept(@NonNull Request request, Action tag) {
        if (tag.getType().equals(PUSH_REGISTER)) {
            return request.newBuilder()
                    // Add query parameter:
                    .url(Uri.parse(request.url().toString())
                            .buildUpon()
                            .appendQueryParameter("testParameter", "PUSH_REGISTER").toString())

                    // Add additional header:
                    .addHeader("testHeader", "PUSH_REGISTER")

                    // Construct the updated request:
                    .build();
        } else if (tag.getType().equals(PUSH_AUTHENTICATE)) {
            return request.newBuilder()
                    // Add query parameter:
                    .url(Uri.parse(request.url().toString())
                            .buildUpon()
                            .appendQueryParameter("testParameter", "PUSH_AUTHENTICATE").toString())

                    // Add additional header:
                    .addHeader("testHeader", "PUSH_AUTHENTICATE")

                    // Construct the updated request:
                    .build();
        }
        return request;
    }
}

