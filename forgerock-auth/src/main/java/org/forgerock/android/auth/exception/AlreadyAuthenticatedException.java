/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

/**
 * Exception thrown when the user is trying to authenticate again.
 */
public class AlreadyAuthenticatedException extends Exception {

    public AlreadyAuthenticatedException() {
    }

    public AlreadyAuthenticatedException(String message) {
        super(message);
    }

    public AlreadyAuthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyAuthenticatedException(Throwable cause) {
        super(cause);
    }
}
