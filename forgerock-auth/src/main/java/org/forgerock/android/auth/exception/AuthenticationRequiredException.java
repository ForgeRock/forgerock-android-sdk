/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

public class AuthenticationRequiredException extends Exception {

    public AuthenticationRequiredException(Throwable cause) {
        super(cause);
    }

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
