/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

/**
 * Exception that is thrown when the user's authentication session timed out and cause the authentication module to
 * be authenticated to <b>fail</b>.
 */
public class AuthenticationTimeoutException extends ApiException {

    /**
     * {@inheritDoc}
     */
    public AuthenticationTimeoutException(int statusCode, String error, String description) {
        super(statusCode, error, description);
    }

}
