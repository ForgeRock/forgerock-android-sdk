/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import lombok.RequiredArgsConstructor;

/**
 * Interceptor to intercept the received token.
 */
@RequiredArgsConstructor
class SingleSignOnInterceptor implements Interceptor<SSOToken> {

    private final SessionManager sessionManager;

    @Override
    public void intercept(final Chain chain, SSOToken token) {
        if (token == null) {
            // trigger the tree with noSession parameter, we don't destroy the existing session.
            chain.proceed(token);
            return;
        }
        Token storedToken = sessionManager.getSingleSignOnManager().getToken();
        //If token changed, we need to revoke Access Token
        if (!token.equals(storedToken)) {
            sessionManager.getTokenManager().revoke(null);
            sessionManager.getSingleSignOnManager().persist(token);
        }
        chain.proceed(token);
    }

}
