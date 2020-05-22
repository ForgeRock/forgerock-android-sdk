/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

/**
 * Represents an error in parsing a Configuration URI, or in creating a Configuration
 * URI from a set of attributes.
 */
public class MechanismParsingException extends Exception {

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     * @param throwable The throwable cause of the exception.
     */
    public MechanismParsingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     */
    public MechanismParsingException(String detailMessage) {
        super(detailMessage);
    }

}
