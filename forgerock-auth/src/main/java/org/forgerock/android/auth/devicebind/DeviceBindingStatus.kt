/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

/**
 * State of the Device Binding errors
 */

sealed class DeviceBindingStatus(var message: String, val clientError: String, val errorCode: Int? = null)
data class Timeout(private val errorMessage: String = "Biometric Timeout", private val errorType: String = "Timeout"): DeviceBindingStatus(errorMessage, errorType)
data class Abort(private val errorMessage: String = "User Terminates the Authentication",  private val errorType: String = "Abort", private val code: Int? = null): DeviceBindingStatus(errorMessage, errorType, code)
data class Unsupported(private var errorMessage: String? = "Device not supported. Please verify the biometric or Pin settings", private val errorType: String = "Unsupported"):
    DeviceBindingStatus(errorMessage ?: "Failed to generate keypair or sign the transaction", errorType)
data class UnRegister(private val errorMessage: String = "PublicKey or PrivateKey Not found in Device", private val errorType: String = "Unsupported"): DeviceBindingStatus(errorMessage, errorType)
object Success: DeviceBindingStatus("", "")


/**
 * Exceptions for device binding
 */
class DeviceBindingException @JvmOverloads constructor(
    val status: DeviceBindingStatus?
) : Exception(status?.message)
