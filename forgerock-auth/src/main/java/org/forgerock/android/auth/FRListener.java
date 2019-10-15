/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

/**
 * Listener to listen for event
 *
 * @param <T> The type of the result
 */
public interface FRListener<T> {

    /**
     * Called when an asynchronous call completes successfully.
     *
     * @param result the value returned
     */
    void onSuccess(T result);

    /**
     * Called when an asynchronous call fails to complete.
     *
     * @param e the reason for failure
     */
    void onException(Exception e);

}
