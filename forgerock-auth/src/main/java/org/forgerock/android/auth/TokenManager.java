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
     * Refresh the {@link AccessToken}, force token refresh, no matter the stored {@link AccessToken} is expired or not
     * refresh the token and persist it.
     *
     * @return The refreshed {@link AccessToken}
     * @throws AuthenticationRequiredException When failed to Refresh the {@link AccessToken}
     */

    AccessToken refresh() throws AuthenticationRequiredException;

    /**
     * Refresh the {@link AccessToken} asynchronously, force token refresh, no matter the stored {@link AccessToken} is expired or not
     * refresh the token and persist it.
     *
     * @param listener Listener to listen for refresh event.
     * @throws AuthenticationRequiredException When failed to Refresh the {@link AccessToken}
     */
    void refresh(FRListener<AccessToken> listener) throws AuthenticationRequiredException;

    /**
     * Get the {@link AccessToken},
     *
     * <p>
     * If the stored {@link AccessToken} is expired, auto refresh the token, during token refresh
     * network request is required, caller should run this method from Worker Thread instead of Main Thread.
     *
     * @return The refreshed {@link AccessToken}
     * @throws AuthenticationRequiredException When failed to Refresh the {@link AccessToken}
     */
    AccessToken getAccessToken() throws AuthenticationRequiredException;

    /**
     * Get the {@link AccessToken} asynchronously,
     *
     * <p>
     * If the stored {@link AccessToken} is expired, auto refresh the token
     *
     * @param tokenListener Listener to listen for get access token event.
     */
    void getAccessToken(FRListener<AccessToken> tokenListener);

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
