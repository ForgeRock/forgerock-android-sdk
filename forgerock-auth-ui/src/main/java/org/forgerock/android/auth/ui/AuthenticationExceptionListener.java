/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import org.forgerock.android.auth.exception.AuthenticationException;

/**
 * Listener to listen for {@link AuthenticationException} event during authentication
 */
public interface AuthenticationExceptionListener {

    void onAuthenticationException(AuthenticationException e);
}
