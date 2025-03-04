/*
 * Copyright (c) 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

import org.forgerock.android.auth.PushMechanism;

import java.util.List;

/**
 * Represents an error in update the device token for a Push mechanism. The affected Push mechanism
 * are provided in the exception.
 */
public class MechanismUpdatePushTokenException extends MechanismCreationException {

    private final List<PushMechanism> pushMechanisms;

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     * @param pushMechanisms The affected Push mechanisms.
     */
    public MechanismUpdatePushTokenException(String detailMessage, List<PushMechanism> pushMechanisms) {
        super(detailMessage);
        this.pushMechanisms = pushMechanisms;
    }

    /**
     * Get the affected Push mechanisms.
     * @return The affected Push mechanisms.
     */
   public List<PushMechanism> getPushMechanisms() {
        return pushMechanisms;
   }

}
