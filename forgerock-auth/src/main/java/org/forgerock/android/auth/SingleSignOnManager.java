/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.util.Collection;

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
     * Persist the Cookies to storage
     *
     * @param cookies The cookies `Set-Cookie` HTTP header value
     */
    void persist(Collection<String> cookies);


    /**
     * Remove the stored {@link Token} and Cookies
     */
    void clear();

    /**
     * Retrieve the {@link Token}
     * @return The SSO Token
     */
    SSOToken getToken();

    /**
     * Retrieve the Stored cookies
     * @return The Cookies
     */
    Collection<String> getCookies();


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

    /**
     * Check if broadcast to other Apps in the SSO Group is enabled.
     */
    default boolean isBroadcastEnabled() {
        return false;
    }

}
