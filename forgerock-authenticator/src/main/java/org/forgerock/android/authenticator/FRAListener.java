/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.authenticator;

/**
 * Listener to handle network connection feedback
 *
 * @param <T> The type of the result
 */
public abstract class FRAListener<T> {

    /**
     * Called when the connection asynchronous call completes successfully.
     * @param result the value returned
     */
    public abstract void onSuccess(T result);

    /**
     * Called when the connection asynchronous call fails to complete.
     * @param e the reason for failure
     */
    public abstract void onException(Exception e);

}
