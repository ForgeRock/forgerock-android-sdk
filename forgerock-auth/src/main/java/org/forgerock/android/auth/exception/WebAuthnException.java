/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

/**
 * Exception that is thrown when the webauthn registration or authentication failed
 */
public class WebAuthnException extends Exception {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public WebAuthnException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.
     *
     * @param message the detail message
     * @param cause   the cause
     * @since 1.4
     */
    public WebAuthnException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause the cause
     */
    public WebAuthnException(Throwable cause) {
        super(cause);
    }
}
