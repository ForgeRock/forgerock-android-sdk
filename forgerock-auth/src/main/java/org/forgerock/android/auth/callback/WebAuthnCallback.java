/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.forgerock.android.auth.Node;

/**
 * Interface for WebAuthn Related callback
 */
public interface WebAuthnCallback {

    /**
     * Set value to the {@link HiddenValueCallback} which associated with the WebAuthn related
     * Callback.
     *
     * @param node  The Node
     * @param value The Value to set to the {@link HiddenValueCallback}
     */
    default void setHiddenCallbackValue(Node node, String value) {
        for (Callback callback : node.getCallbacks()) {
            if (callback instanceof HiddenValueCallback) {
                if (((HiddenValueCallback) callback).getId().equals("webAuthnOutcome")) {
                    ((HiddenValueCallback) callback).setValue(value);
                }
            }
        }
    }
}
