/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.auth;

import android.net.Uri;

import androidx.annotation.NonNull;

import org.forgerock.android.auth.AccessToken;
import org.forgerock.android.auth.Action;
import org.forgerock.android.auth.Config;
import org.forgerock.android.auth.FRRequestInterceptor;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Request;
import org.forgerock.android.auth.RequestInterceptor;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;

import static org.forgerock.android.auth.Action.AUTHENTICATE;
import static org.forgerock.android.auth.Action.START_AUTHENTICATE;

/**
 * Sample {@link RequestInterceptor} to add ForceAuth
 */
public class IDTokenRequestInterceptor implements FRRequestInterceptor<Action> {

    @NonNull
    @Override
    public Request intercept(@NonNull Request request, Action tag) {

        if (tag.getType().equals(START_AUTHENTICATE)) {
            if (FRUser.getCurrentUser() != null) {
                try {
                    AccessToken accessToken = FRUser.getCurrentUser().getAccessToken();
                    if (accessToken.getIdToken() != null) {
                        return request.newBuilder()
                                .url(Uri.parse(request.url().toString())
                                        .buildUpon()
                                        .appendQueryParameter("iPlanetDirectoryPro", accessToken.getIdToken())
                                        .toString())
                                .build();
                    }
                } catch (AuthenticationRequiredException e) {
                    //Do nothing
                }
            }
        }
        return request;
    }
}
