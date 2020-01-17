/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.exception.AuthenticationRequiredException;

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
     * Refresh the {@link AccessToken} asynchronously, force token refresh, no matter the stored {@link AccessToken} is expired or not
     * refresh the token and persist it.
     *
     * @param accessToken AccessToken
     * @param listener Listener to listen for refresh event.
     * @throws AuthenticationRequiredException When failed to Refresh the {@link AccessToken}
     */
    void refresh(AccessToken accessToken, FRListener<AccessToken> listener) throws AuthenticationRequiredException;

    /**
     * Get the {@link AccessToken} asynchronously,
     *
     *
     * <p>
     * If the stored {@link AccessToken} is expired, auto refresh the token
     *
     * @param accessTokenVerifier Verifier to verify the access token.
     * @param tokenListener Listener to listen for get access token event.
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
