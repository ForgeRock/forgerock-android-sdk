/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.forgerock.android.auth.exception.AuthorizeException;

import java.util.Collections;

import lombok.RequiredArgsConstructor;

/**
 * Interceptor to intercept the received token and exchange to {@link AccessToken}.
 */
@RequiredArgsConstructor
class OAuthInterceptor implements Interceptor<SSOToken> {

    private final SessionManager sessionManager;

    @Override
    public void intercept(final Chain chain, SSOToken token) {
        if (token == null) {
            Listener.onException(chain.getListener(), new AuthenticationRequiredException("Authentication Required."));
            return;
        }
        sessionManager.getTokenManager().exchangeToken(token, Collections.emptyMap(), new FRListener<AccessToken>() {
            @Override
            public void onSuccess(AccessToken accessToken) {
                chain.proceed(accessToken);
            }

            @Override
            public void onException(Exception e) {
                if (e instanceof AuthorizeException) {
                    //We clean up the SSOToken if we are not able to use the SSOToken to exchange authorization code.
                    sessionManager.getSingleSignOnManager().clear();
                   Listener.onException(chain.getListener(), new AuthenticationRequiredException(e));
                } else {
                    Listener.onException(chain.getListener(), e);
                }
            }
        });
    }
}
