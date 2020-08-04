/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import org.forgerock.android.auth.callback.Callback;

/**
 * Controller to collect Callback data for {@link org.forgerock.android.auth.ui.callback.CallbackFragment}
 * and control the Auth Service Action, the parent activity or parent fragment of the CallbackFragment should
 * implement this interface to manage the embedded CallbackFragment.
 * see {@link AdaptiveCallbackFragment}
 */
public interface CallbackController {

    /**
     * Move to the next node in the tree
     */
    void next();

    /**
     * Notify when callback data are collected.
     *
     * @param callback The Callback
     */
    void onDataCollected(Callback callback);

    /**
     * Cancel the current authentication
     *
     * @param e Exception when cancelling the authentication process.
     */
    void cancel(Exception e);

    /**
     * Suspend the current authentication
     */
    void suspend();


}
