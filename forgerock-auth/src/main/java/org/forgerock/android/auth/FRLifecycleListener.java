/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import java.util.Collection;

/**
 * Listener to listen for FR SDK Event
 */
public interface FRLifecycleListener {

    /**
     * Interface definition for a listener to be invoked when the {@link SSOToken} changed.
     *
     * @param ssoToken The updated SSOToken
     */
    void onSSOTokenUpdated(SSOToken ssoToken);

    /**
     * Interface definition for a listener to be invoked when the cookies changed.
     *
     * @param cookies The updated Cookies
     */
    void onCookiesUpdated(Collection<String> cookies);

    /**
     * Interface definition for a listener to be invoked when the logout.
     */
    void onLogout();

}
