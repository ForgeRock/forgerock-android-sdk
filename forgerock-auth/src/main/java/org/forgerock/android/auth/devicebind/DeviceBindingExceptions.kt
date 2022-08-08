package org.forgerock.android.auth.devicebind

/**
 * Exceptions for device binding
 */
class DeviceBindSigningException @JvmOverloads constructor(
    val error: String? = null,
) : Exception(error)

class DeviceKeyPairCreationException @JvmOverloads constructor(
    val error: String? = null
) : Exception(error)

class DeviceBindBiometricException @JvmOverloads constructor(
    val error: String? = null,
) : Exception(error)