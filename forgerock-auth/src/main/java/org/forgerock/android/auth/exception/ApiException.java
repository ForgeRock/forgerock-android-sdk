/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

/**
 * Base exception for all API exceptions.
 *
 */
public class ApiException extends Exception {

    private final int statusCode;
    private final String error;

    /**
     * Constructs a new ApiException with specified status code, error and description.
     *
     * @param statusCode  The status code of the exception. Maps to HTTP status codes.
     * @param error       The error/name of the exception.
     * @param description The reason and description for the exception.
     */
    public ApiException(final int statusCode, final String error, final String description
    ) {
        this(statusCode, error, description, null);
    }

    /**
     * Constructs a new ApiException with specified status code, error and description.
     *
     * @param statusCode  The status code of the exception. Maps to HTTP status codes.
     * @param error       The error/name of the exception.
     * @param description The reason and description for the exception.
     * @param cause       The underlying cause of the error case.
     */
    public ApiException(final int statusCode, final String error, final String description,
                        Throwable cause) {
        super(description, cause);
        this.statusCode = statusCode;
        this.error = error;
    }

    /**
     * Gets the status code of the exception.
     *
     * @return The status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the error/name of the exception.
     *
     * @return The error.
     */
    public String getError() {
        return error;
    }

}
