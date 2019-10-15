/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Manage SSO related attributes
 */
public interface SingleSignOnManager {

    /**
     * Persist the {@link Token} to storage
     *
     * @param token The SSO Token
     */
    void persist(SSOToken token);


    /**
     * Remove the stored {@link Token}
     */
    void clear();

    /**
     * Retrieve the {@link Token}
     * @return The SSO Token
     */
    SSOToken getToken();

    /**
     * Check if token exists in the storage.
     *
     * @return True if token exists, otherwise false
     */

    boolean hasToken();

    /**
     * Revoke the SSO Session
     *
     * @param listener Listener to listen for token revocation event.
     */
    void revoke(FRListener<Void> listener);

}
