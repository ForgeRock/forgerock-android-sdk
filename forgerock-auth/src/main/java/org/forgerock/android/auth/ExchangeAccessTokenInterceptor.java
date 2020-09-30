/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import net.openid.appauth.AuthorizationResponse;

import lombok.RequiredArgsConstructor;

import static java.util.Collections.emptyMap;

/**
 * Interceptor to intercept the received authorization code and exchange to {@link AccessToken}.
 */
@RequiredArgsConstructor
class ExchangeAccessTokenInterceptor implements Interceptor<AuthorizationResponse> {

    private final TokenManager tokenManager;

    @Override
    public void intercept(final Chain chain, final AuthorizationResponse response) {

        PKCE pkce = new PKCE(response.request.codeVerifierChallenge,
                response.request.codeVerifierChallengeMethod,
                response.request.codeVerifier);

        tokenManager.exchangeToken(response.authorizationCode, pkce, emptyMap(),
                new FRListener<AccessToken>() {
                    @Override
                    public void onSuccess(AccessToken result) {
                        chain.proceed(result);
                    }

                    @Override
                    public void onException(Exception e) {
                        Listener.onException(chain.getListener(), e);
                    }
                });

    }

}
