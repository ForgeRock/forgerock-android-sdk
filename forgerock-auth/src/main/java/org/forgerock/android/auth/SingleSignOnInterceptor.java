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
     * <p>Mirrors the iOS SDK's {@code KeychainManager.handleSessionToken} behavior: when there is
     * no existing stored session token, the existing session does not apply — this is the
     * centralized login case (the SDK was authenticated via the browser/AppAuth flow, which never
     * stores an SSO token). If an OAuth2.0 access token from the previous authentication still
     * exists, the stale token set is revoked (without ending the session) so the SDK does not hold
     * credentials of a different user/session than the one just established; then the newly
     * received session token is persisted and the lifecycle listeners notified, consistent with
     * {@link RetrieveAccessTokenInterceptor}'s handling of centralized login.
     *
     * @param chain The interceptor chain
     * @param token The session token received from the previous chain
     */
    @Override
    public void intercept(final Chain chain, SSOToken token) {
        if (token == null || token.getValue().isEmpty()) {
            // trigger the tree with noSession parameter, we don't destroy the existing session.
            chain.proceed(token);
            return;
        }
        Token storedToken = sessionManager.getSingleSignOnManager().getToken();
        if (storedToken == null) {
            AccessToken accessToken = sessionManager.getTokenManager().getAccessToken();
            if (accessToken != null) {
                //There is no existing session token but there is an access token,
                //revoke the stale OAuth2.0 token set obtained by the previous (centralized login)
                //authentication to avoid stale credentials, without ending the session.
                sessionManager.getTokenManager().revoke(null);
            }
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
