/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.exception;

import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse;
import com.google.android.gms.fido.fido2.api.common.ErrorCode;

import lombok.Getter;

/**
 * An Exception representation of {@link AuthenticatorErrorResponse}
 */
@Getter
public class WebAuthnResponseException extends Exception {

    private final ErrorCode errorCode;
    private final int errorCodeAsInt;

    /**
     * Constructs a new exception with the specified {@link AuthenticatorErrorResponse}
     *
     * @param authenticatorErrorResponse the AuthenticatorErrorResponse
     */
    public WebAuthnResponseException(AuthenticatorErrorResponse authenticatorErrorResponse) {
        super(authenticatorErrorResponse.getErrorMessage());
        this.errorCode = authenticatorErrorResponse.getErrorCode();
        this.errorCodeAsInt = authenticatorErrorResponse.getErrorCodeAsInt();
    }

    /**
     * Constructs a new exception with the specified {@link AuthenticatorErrorResponse}
     *
     * @param errorCode the ErrorCode
     * @param message the detail message
     */
    public WebAuthnResponseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorCodeAsInt = errorCode.getCode();
    }

    /**
     * Server error string representation of this error.
     * Result to the Client Error outcome for WebAuthn Registration Node or WebAuthn Authentication Node
     *
     * @return Server error string representation of this error.
     */
    public String toServerError() {
        return "ERROR::" + toServerErrorCode() + ":" + getMessage();
    }

    private String toServerErrorCode() {
        switch (errorCode) {
            case DATA_ERR:
                return "DataError";
            case CONSTRAINT_ERR:
                return "ConstraintError";
            case ENCODING_ERR:
                return "EncodingError";
            case TIMEOUT_ERR:
                return "TimeoutError";
            case NETWORK_ERR:
                return "NetworkError";
            case UNKNOWN_ERR:
                return "UnknownError";
            case INVALID_STATE_ERR:
                return "InvalidStateError";
            case NOT_SUPPORTED_ERR:
                return "NotSupportedError";
            case ABORT_ERR:
                return "AbortError";
            case SECURITY_ERR:
                return "SecurityError";
            case NOT_ALLOWED_ERR:
                return "NotAllowedError";
            case ATTESTATION_NOT_PRIVATE_ERR:
                return "UnknownError";
            default:
                return "UnknownError";

        }
    }
}
