/*
 * Copyright (c) 2020 - 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.exception

/**
 * Exception that is thrown when the authorize failed
 */
class AuthorizeException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
