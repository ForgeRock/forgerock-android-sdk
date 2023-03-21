/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

import org.forgerock.android.auth.policy.FRAPolicy;

/**
 * Represents an error while registering a Mechanism due policy violation.
 */
public class MechanismPolicyViolationException extends Exception {

    private FRAPolicy cause;

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     * @param cause The policy which caused the exception.
     */
    public MechanismPolicyViolationException(String detailMessage, FRAPolicy cause) {
        super(detailMessage);
        this.cause = cause;
    }

    public FRAPolicy getCausingPolicy() {
        return cause;
    }

}
