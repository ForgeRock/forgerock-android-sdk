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

    public WebAuthnException(String message) {
        super(message);
    }

    public WebAuthnException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebAuthnException(Throwable cause) {
        super(cause);
    }
}
