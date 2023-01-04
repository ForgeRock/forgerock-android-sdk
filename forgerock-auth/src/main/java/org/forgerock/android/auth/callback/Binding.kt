/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import kotlinx.coroutines.CancellationException
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus
import org.forgerock.android.auth.devicebind.DeviceBindingException
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Device Binding interface to provide utility method for [DeviceBindingCallback] and [DeviceSigningVerifierCallback]
 */
private val tag = Binding::class.java.simpleName

interface Binding {

    /**
     * Create the interface for the Authentication type(Biometric, Biometric_Fallback, none)
     * @param type The Device Binding Authentication Type
     * @return The recommended  [DeviceAuthenticator] that can handle the provided [DeviceBindingAuthenticationType]
     */
    fun getDeviceAuthenticator(type: DeviceBindingAuthenticationType): DeviceAuthenticator =
        type.getAuthType()

    /**
     * Get Expiration date for the signed token, claim "exp" will be set to the JWS.
     *
     * @return The expiration date
     */
    fun getExpiration(timeout: Int?): Date {
        val date = Calendar.getInstance();
        date.add(Calendar.SECOND, timeout ?: 60)
        return date.time;
    }

    /**
     * Convert timeout in seconds to [Duration]
     * @param timeout the timeout in seconds
     */
    fun getDuration(timeout: Int?): Duration {
        return (timeout?.toLong() ?: 60L).toDuration(DurationUnit.SECONDS)
    }

    /**
     * Handle all the errors for the device binding.
     */
    fun handleException(e: Throwable) {
        when (e) {
            is DeviceBindingException -> {
                handleException(e.status, e)
            }
            is CancellationException -> {
                throw e
            }
            else -> {
                handleException(DeviceBindingErrorStatus.Unsupported(errorMessage = e.message
                    ?: ""), e)
            }
        }
    }

    /**
     * Handle all the errors for the device binding.
     *
     * @param status  DeviceBindingStatus(timeout,Abort, unsupported)
     */
    fun handleException(status: DeviceBindingErrorStatus,
                        e: Throwable) {

        setClientError(status.clientError)
        Logger.error(tag, e, status.message)
        throw e
    }

    fun setClientError(clientError: String?)

}