/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import lombok.RequiredArgsConstructor;

/**
 * Interceptor to intercept the received token and persist it.
 */
@RequiredArgsConstructor
class AccessTokenStoreInterceptor implements Interceptor<AccessToken> {

    private final TokenManager tokenManager;

    @Override
    public void intercept(final Chain chain, AccessToken token) {
        if (!token.isPersisted()) {
            tokenManager.persist(token);
        }
        chain.proceed(token);
    }

}
