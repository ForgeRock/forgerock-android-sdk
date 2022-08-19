package org.forgerock.android.auth.devicebind

/**
 * Exceptions for device binding
 */
class BiometricTimeOutException @JvmOverloads constructor(
    val error: String? = null,
) : Exception(error)

class BiometricErrorException @JvmOverloads constructor(
    val error: String? = null,
) : Exception(error)

class DeviceBindingException @JvmOverloads constructor(
    val error: String? = null,
) : Exception(error)