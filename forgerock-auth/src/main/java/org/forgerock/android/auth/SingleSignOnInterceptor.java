/*
 * Copyright (c) 2019 - 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import net.openid.appauth.AppAuthConfiguration;

import lombok.RequiredArgsConstructor;

/**
 * Interceptor to intercept the received token.
 */
@RequiredArgsConstructor
class SingleSignOnInterceptor implements Interceptor<SSOToken> {

    private final SessionManager sessionManager;

    /**
     * Intercept the received session token.
     *
     * <p>When there is no existing stored session token, the existing session does not apply —
     * this is the centralized login case (the SDK was authenticated via the browser/AppAuth flow,
     * which never stores an SSO token). The newly received session token is persisted and the
     * lifecycle listeners notified, without revoking the OAuth2.0 tokens, consistent with
     * {@link RetrieveAccessTokenInterceptor}'s handling of centralized login.
     *
     * @param chain The interceptor chain
     * @param token The session token received from the previous chain
     */
    @Override
    public void intercept(final Chain chain, SSOToken token) {
        if (token == null) {
            // trigger the tree with noSession parameter, we don't destroy the existing session.
            chain.proceed(token);
            return;
        }
        Token storedToken = sessionManager.getSingleSignOnManager().getToken();
        if (storedToken == null) {
            //No existing session token, the existing session does not apply,
            //it considers as Centralize login, no validation on session token binding.
            //Persist the new session token without revoking the OAuth2.0 tokens.
            sessionManager.getSingleSignOnManager().persist(token);
            FRLifecycle.dispatchSSOTokenUpdated(token);
            chain.proceed(token);
            return;
        }
        //If token changed, we need to revoke Access Token
        if (!token.equals(storedToken)) {
            sessionManager.getTokenManager().revokeAndEndSession(null);
            sessionManager.getSingleSignOnManager().persist(token);
            FRLifecycle.dispatchSSOTokenUpdated(token);
        }
        chain.proceed(token);
    }

}
