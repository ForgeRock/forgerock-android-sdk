package org.forgerock.android.auth.devicebind

/**
 * Exceptions for device binding
 */

class DeviceBindingException @JvmOverloads constructor(
    val error: String? = "",
    val errorCode: Int? = null,
) : Exception(error)


enum class DeviceBindingError(val message: String) {
    Timeout("Biometric Timeout"),
    Abort("User Terminates the Authentication"),
    Unsupported("Device not supported. Please verify the biometric or Pin settings"),
    KeyCreationAndSign("Failed to generate keypair or sign the transaction")
}