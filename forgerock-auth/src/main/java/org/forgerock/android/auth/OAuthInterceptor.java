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
class OAuthInterceptor implements Interceptor<SSOToken> {

    private final OAuth2Client oAuth2Client;

    @Override
    public void intercept(final Chain chain, SSOToken token) {
        oAuth2Client.exchangeToken(token, new FRListener<AccessToken>() {
            @Override
            public void onSuccess(AccessToken accessToken) {
                chain.proceed(accessToken);
            }

            @Override
            public void onException(Exception e) {
                chain.getListener().onException(e);
            }
        });
    }

}
