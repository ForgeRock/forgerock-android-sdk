/*
 * Copyright (c) 2020 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

/**
 * Exception thrown when there's an error with the Block Store operations.
 */
public class BlockStoreException extends Exception {
    
    /**
     * Constructor with message.
     *
     * @param message The error message.
     */
    public BlockStoreException(String message) {
        super(message);
    }
    
    /**
     * Constructor with message and cause.
     *
     * @param message The error message.
     * @param cause The cause of the exception.
     */
    public BlockStoreException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructor with cause.
     *
     * @param cause The cause of the exception.
     */
    public BlockStoreException(Throwable cause) {
        super(cause);
    }
}
