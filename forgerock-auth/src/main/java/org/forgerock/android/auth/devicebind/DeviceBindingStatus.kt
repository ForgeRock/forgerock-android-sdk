/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import java.security.PrivateKey
import java.security.Signature

/**
 * State of the Device Binding errors
 */
private const val TIMEOUT = "Timeout"
private const val ABORT = "Abort"
private const val UNSUPPORTED = "Unsupported"
private const val CLIENT_NOT_REGISTERED = "ClientNotRegistered"

sealed interface DeviceBindingStatus

abstract class DeviceBindingErrorStatus(var message: String,
                                        val clientError: String,
                                        val errorCode: Int? = null) :
    DeviceBindingStatus {

    data class Timeout(private val errorMessage: String = "Authentication Timeout",
                       private val errorType: String = TIMEOUT,
                       private val code: Int? = null) :
        DeviceBindingErrorStatus(errorMessage, errorType, code)


    data class Abort(private val errorMessage: String = "User Terminates the Authentication",
                     private val errorType: String = ABORT,
                     private val code: Int? = null) :
        DeviceBindingErrorStatus(errorMessage, errorType, code)

    data class Unsupported(private var errorMessage: String = "Device not supported. Please verify the biometric or Pin settings",
                           private val errorType: String = UNSUPPORTED,
                           private val code: Int? = null) :
        DeviceBindingErrorStatus(errorMessage, errorType, code)

    data class ClientNotRegistered(private val errorMessage: String = "PublicKey or PrivateKey Not found in Device",
                                   private val errorType: String = CLIENT_NOT_REGISTERED,
                                   private val code: Int? = null) :
        DeviceBindingErrorStatus(errorMessage, errorType, code)

    data class UnAuthorize(private val errorMessage: String = "Invalid Credentials",
                           private val errorType: String = ABORT,
                           private val code: Int? = null) :
        DeviceBindingErrorStatus(errorMessage, errorType, code)

    data class Unknown(private val errorMessage: String = "Unknown",
                       private val errorType: String = ABORT,
                       private val code: Int? = null) :
        DeviceBindingErrorStatus(errorMessage, errorType, code)

    data class InvalidCustomClaims(private val errorMessage: String = "Invalid Custom Claims",
                       private val errorType: String = ABORT,
                       private val code: Int? = null) :
        DeviceBindingErrorStatus(errorMessage, errorType, code)
}

/**
 * Represent the success status after [DeviceAuthenticator.authenticate]
 *
 * @property privateKey The unlocked private key
 * @property signature The unlocked signature
 */
data class Success(val privateKey: PrivateKey, val signature: Signature? = null) : DeviceBindingStatus

/**
 * Exceptions for device binding
 */
class DeviceBindingException : Exception {
    val status: DeviceBindingErrorStatus

    constructor(status: DeviceBindingErrorStatus) : super(status.message) {
        this.status = status
    }
    constructor(status: DeviceBindingErrorStatus, cause: Throwable?) : super(status.message,
        cause) {
        this.status = status
    }
}
