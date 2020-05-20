/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

import org.forgerock.android.auth.Mechanism;

/**
 * Represents an error in setting up a mechanism, caused by a matching mechanism already existing.
 */
public class DuplicateMechanismException extends MechanismCreationException {

    private Mechanism cause;

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     */
    public DuplicateMechanismException(String detailMessage, Mechanism cause) {
        super(detailMessage);
        this.cause = cause;
    }

    public Mechanism getCausingMechanism() {
        return cause;
    }

}
