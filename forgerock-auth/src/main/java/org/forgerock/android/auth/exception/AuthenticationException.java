/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

/**
 * Exception that is thrown when the user-entered credentials cause the authentication module to
 * be authenticated to <b>fail</b>. For example, invalid Username/Password or Invalid OTP.
 */
public class AuthenticationException extends ApiException {

    /**
     * {@inheritDoc}
     */
    public AuthenticationException(int statusCode, String error, String description) {
        super(statusCode, error, description);
    }

}
