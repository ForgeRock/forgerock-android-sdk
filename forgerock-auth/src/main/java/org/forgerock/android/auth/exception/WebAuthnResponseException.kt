/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.exception

import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.ErrorCode

/**
 * An Exception representation of [AuthenticatorErrorResponse]
 */
class WebAuthnResponseException(authenticatorErrorResponse: AuthenticatorErrorResponse) :
    Exception(authenticatorErrorResponse.errorMessage) {
    val errorCode: ErrorCode
    val errorCodeAsInt: Int

    init {
        errorCode = authenticatorErrorResponse.errorCode
        errorCodeAsInt = authenticatorErrorResponse.errorCodeAsInt
    }

    /**
     * Server error string representation of this error.
     * Result to the Client Error outcome for WebAuthn Registration Node or WebAuthn Authentication Node
     *
     * @return Server error string representation of this error.
     */
    fun toServerError(): String {
        return "ERROR::" + toServerErrorCode() + ":" + message
    }

    private fun toServerErrorCode(): String {
        return when (errorCode) {
            ErrorCode.DATA_ERR -> "DataError"
            ErrorCode.CONSTRAINT_ERR -> "ConstraintError"
            ErrorCode.ENCODING_ERR -> "EncodingError"
            ErrorCode.TIMEOUT_ERR -> "TimeoutError"
            ErrorCode.NETWORK_ERR -> "NetworkError"
            ErrorCode.UNKNOWN_ERR -> "UnknownError"
            ErrorCode.INVALID_STATE_ERR -> "InvalidStateError"
            ErrorCode.NOT_SUPPORTED_ERR -> "NotSupportedError"
            ErrorCode.ABORT_ERR -> "AbortError"
            ErrorCode.SECURITY_ERR -> "SecurityError"
            ErrorCode.NOT_ALLOWED_ERR -> "NotAllowedError"
            ErrorCode.ATTESTATION_NOT_PRIVATE_ERR -> "UnknownError"
            else -> "UnknownError"
        }
    }
}