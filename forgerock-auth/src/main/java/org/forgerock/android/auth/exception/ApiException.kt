/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.exception

import java.lang.Exception

/**
 * Base exception for all API exceptions.
 * Constructs a new ApiException with specified status code, error and description.
 *
 * @param statusCode  The status code of the exception. Maps to HTTP status codes.
 * @param error       The error/name of the exception.
 * @param message The reason and description for the exception.
 */
open class ApiException(val statusCode: Int, val error: String, message: String?) : Exception(message) {

    override fun toString(): String {
        return "ApiException{statusCode=$statusCode, error='$error', description='$message'}"
    }
}