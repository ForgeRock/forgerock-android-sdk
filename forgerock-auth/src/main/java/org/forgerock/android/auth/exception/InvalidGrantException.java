/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

/**
 * Exception that is thrown when server response with invalid-grant
 */
public class InvalidGrantException extends Exception {

    public InvalidGrantException(String message, Throwable cause) {
        super(message, cause);
    }
}
