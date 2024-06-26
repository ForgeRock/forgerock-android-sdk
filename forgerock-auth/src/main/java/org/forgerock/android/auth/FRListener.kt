/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

/**
 * A listener interface for receiving completion events from asynchronous operations.
 *
 * The class that is interested in processing a completion event implements this interface,
 * and the object created with that class is registered with the asynchronous operation,
 * using the operation's `setListener` method. When the operation completes,
 * that object's `onSuccess` or `onException` method is invoked.
 *
 * @param <T> The type of the result.
 */
interface FRListener<T> {
    /**
     * Invoked when an asynchronous operation completes successfully.
     *
     * @param result The result of the operation.
     */
    fun onSuccess(result: T)

    /**
     * Invoked when an asynchronous operation fails.
     *
     * @param e The exception that caused the operation to fail.
     */
    fun onException(e: Exception)
}

/**
 * A no-op implementation of the FRListener interface that does nothing when
 * the asynchronous operation completes. This can be used when you want to start
 * an asynchronous operation, but don't care about the result.
 *
 * @param <T> The type of the result.
 */
class DoNothingListener<T> : FRListener<T> {
    /**
     * Does nothing when the operation completes successfully.
     *
     * @param result The result of the operation.
     */
    override fun onSuccess(result: T) {
        // Do nothing
    }

    /**
     * Does nothing when the operation fails.
     *
     * @param e The exception that caused the operation to fail.
     */
    override fun onException(e: Exception) {
        // Do nothing
    }
}