/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

/**
 * Listener to listen for event
 *
 * @param <T> The type of the result </T>
 */
interface FRListener<T> {
    /**
     * Called when an asynchronous call completes successfully.
     *
     * @param result the value returned
     */
    fun onSuccess(result: T)

    /**
     * Called when an asynchronous call fails to complete.
     *
     * @param e the reason for failure
     */
    fun onException(e: Exception)


}