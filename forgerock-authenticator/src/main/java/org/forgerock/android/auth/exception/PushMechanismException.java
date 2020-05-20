/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

/**
 * Represents an error in process a PushMechanism registration or PushMechanism authentication request.
 */
public class PushMechanismException extends Exception {

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     * @param throwable The throwable cause of the exception.
     */
    public PushMechanismException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     */
    public PushMechanismException(String detailMessage) {
        super(detailMessage);
    }

}
