/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import lombok.RequiredArgsConstructor;

/**
 * Interceptor to intercept the received token and exchange to {@link AccessToken}.
 */
@RequiredArgsConstructor
class RetrieveAccessTokenInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    @Override
    public void intercept(final Chain chain, final Object input) {

        tokenManager.getAccessToken(new FRListener<AccessToken>() {
            @Override
            public void onSuccess(AccessToken result) {
                chain.getListener().onSuccess(result);
            }

            @Override
            public void onException(Exception e) {
                //If cannot get the AccessToken, process to next interceptor in the chain.
                chain.proceed(input);
            }
        });
    }

}
