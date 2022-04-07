/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.forgerock.android.auth.exception.InvalidGrantException;

import lombok.RequiredArgsConstructor;

/**
 * Interceptor to intercept the received token and exchange to {@link AccessToken}.
 */
@RequiredArgsConstructor
class RetrieveAccessTokenInterceptor implements Interceptor<SSOToken> {

    private final SingleSignOnManager singleSignOnManager;
    private final TokenManager tokenManager;

    @Override
    public void intercept(final Chain chain, final SSOToken sessionToken) {

        //With Verifier to verify the token is associated with the Session Token
        tokenManager.getAccessToken(accessToken -> {

           if (sessionToken == null) {
               if (accessToken.getSessionToken() != null) {
                   //sessionToken may be deleted, restore it.
                   singleSignOnManager.persist(accessToken.getSessionToken());
               }
               //If sessionToken is null and accessToken's session Token is null
               //it considers as Centralize login, no validation on session token binding.
               return true;
           } else {
               //Verify the accessToken is bound to the right session token.
               //For SSO scenario, the session token may associate with different user.
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
                //If cannot get the AccessToken, process to next interceptor in the chain only if
                //we know we not able to refresh.
                //AccessToken or Refresh token does not exists, or current access token are not valid due to SSO binding
                if (e instanceof InvalidGrantException || e instanceof AuthenticationRequiredException) {
                    chain.proceed(sessionToken);
                } else {
                    Listener.onException(chain.getListener(), e);
                }
            }
        });
    }
}
