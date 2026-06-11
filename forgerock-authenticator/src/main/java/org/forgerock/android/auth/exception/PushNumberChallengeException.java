/*
 * Copyright (c) 2026 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

/**
 * Represents an error that occurs during Push Number Challenge validation,
 * such as when the user selects an incorrect number or the challenge cannot be processed.
 */
public class PushNumberChallengeException extends PushMechanismException {

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     */
    public PushNumberChallengeException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Create a new exception containing a message and a cause.
     * @param detailMessage The message cause of the exception.
     * @param throwable The throwable cause of the exception.
     */
    public PushNumberChallengeException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

}
