/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

/**
 * Represents an error on analysing a FRAPolicy.
 */
public class InvalidPolicyException extends Exception {

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     * @param throwable The throwable cause of the exception.
     */
    public InvalidPolicyException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     */
    public InvalidPolicyException(String detailMessage) {
        super(detailMessage);
    }

}
