/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

/**
 * A sealed class representing a result, which can be either Success or Failure.
 *
 * @param Success The type of the success value.
 * @param Failure The type of the failure value.
 */
internal sealed class Result<out Success, out Failure> {
    /**
     * Represents a failure result.
     *
     * @property value The failure value.
     */
    data class Failure<Failure>(val value: Failure) : Result<Nothing, Failure>()

    /**
     * Represents a success result.
     *
     * @property value The success value.
     */
    data class Success<Success>(val value: Success) : Result<Success, Nothing>()
}