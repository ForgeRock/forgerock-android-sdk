/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.util.Map;

/**
 * Interface to Manage OAuth2 Tokens
 */
public interface TokenManager {

    /**
     * Persist the {@link AccessToken} to storage
     *
     * @param token The AccessToken
     */
    void persist(AccessToken token);

    /**
     * Sends an authorization request to the authorization service.
     *
     * @param token                The SSO Token received with the result of {@link AuthService}
     * @param additionalParameters Additional parameters for inclusion in the authorization endpoint
     * @param listener             Listener that listens to changes resulting from OAuth endpoints .
     */
    void exchangeToken(SSOToken token, final Map<String, String> additionalParameters, final FRListener<AccessToken> listener);

    /**
     * Sends an authorization request to the authorization service.
     *
     * @param code                 The Authorization Code
     * @param pkce                 The Proof Key for Code Exchange
     * @param additionalParameters Additional parameters for inclusion in the token endpoint
     *                             request
     * @param listener             Listener that listens to changes resulting from OAuth endpoints .
     */
    void exchangeToken(String code, PKCE pkce, Map<String, String> additionalParameters, final FRListener<AccessToken> listener);


    /**
     * Refresh the {@link AccessToken} asynchronously, force token refresh, no matter the stored {@link AccessToken} is expired or not
     * refresh the token and persist it.
     *
     * @param accessToken AccessToken
     * @param listener    Listener to listen for refresh event.
     */
    void refresh(AccessToken accessToken, FRListener<AccessToken> listener);

    /**
     * Get the {@link AccessToken} asynchronously,
     *
     *
     * <p>
     * If the stored {@link AccessToken} is expired, auto refresh the token
     *
     * @param accessTokenVerifier Verifier to verify the access token.
     * @param tokenListener       Listener to listen for get access token event.
     */
    void getAccessToken(AccessTokenVerifier accessTokenVerifier, FRListener<AccessToken> tokenListener);

    /**
     * Check if token exists in the storage.
     *
     * @return True if token exists, otherwise false
     */
    boolean hasToken();

    /**
     * Remove the stored {@link AccessToken}
     */
    void clear();

    /**
     * OAuth2 Token Revocation
     *
     * @param listener Listener to listen for token revocation event.
     */
    void revoke(FRListener<Void> listener);


}
