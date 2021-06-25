/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.exception.WebAuthnException;
import org.forgerock.android.auth.exception.WebAuthnResponseException;

/**
 * Listener to listen for WebAuthn Event
 */
public interface WebAuthnListener extends FRListener<String> {

    /**
     * Called when an asynchronous call completes successfully.
     *
     * @param result the value returned
     */
    void onSuccess(String result);

    /**
     * Called when an asynchronous call fails to complete.
     *
     * @param e the reason for failure
     */
    void onException(WebAuthnResponseException e);

    /**
     * Called when asynchronous call fails to complete with unsupport operation.
     *
     * @param e the reason for failure
     */
    void onUnsupported(WebAuthnResponseException e);


}
