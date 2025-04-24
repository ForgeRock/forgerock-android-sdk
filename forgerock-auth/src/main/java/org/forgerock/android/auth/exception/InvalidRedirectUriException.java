/*
 * Copyright (c) 2020 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

public class InvalidRedirectUriException extends Exception {
    public InvalidRedirectUriException(String message) {
        super(message);
    }
}
