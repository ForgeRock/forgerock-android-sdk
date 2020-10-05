/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
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
class RetrieveAccessTokenInterceptor implements Interceptor<SSOToken> {

    private final TokenManager tokenManager;

    @Override
    public void intercept(final Chain chain, final SSOToken sessionToken) {

        //With Verifier to verify the token is associated with the Session Token
        tokenManager.getAccessToken(accessToken -> {
            if (sessionToken == null && accessToken.getSessionToken() == null) {
                return true;
            } else {
                return accessToken.getSessionToken() != null &&
                        accessToken.getSessionToken().equals(sessionToken);
            }
        }, new FRListener<AccessToken>() {
            @Override
            public void onSuccess(AccessToken result) {
                //We don't have to proceed to next, we have the AccessToken already
                //Response to caller immediately
                Listener.onSuccess(chain.getListener(), result);
            }

            @Override
            public void onException(Exception e) {
                //If cannot get the AccessToken, process to next interceptor in the chain.
                chain.proceed(sessionToken);
            }
        });
    }
}
