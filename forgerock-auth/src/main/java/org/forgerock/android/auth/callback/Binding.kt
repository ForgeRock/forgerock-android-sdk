/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.os.OperationCanceledException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.Abort
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.Timeout
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.Unsupported
import org.forgerock.android.auth.devicebind.DeviceBindingException
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val TAG = Binding::class.java.simpleName

/**
 * Device Binding interface to provide utility method for [DeviceBindingCallback] and [DeviceSigningVerifierCallback]
 */
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
                Logger.error(TAG, e, e.message)
                setClientError(e.status.clientError)
                throw e
            }
            is OperationCanceledException -> {
                handleException(DeviceBindingException(Abort(), e))
            }
            is TimeoutCancellationException -> {
                //For Timeout, it is consider subclass of CancellationException, we want developer to ignore
                //CancellationException in case of configuration change but not TimeoutCancellationException
                handleException(DeviceBindingException(Timeout(), e))
            }
            is CancellationException -> {
                throw e
            }
            else -> {
                handleException(DeviceBindingException(Unsupported(), e))
            }
        }
    }

    fun setClientError(clientError: String?)

    /**
     * Default function to identify [DeviceAuthenticator]
     */
    val deviceAuthenticatorIdentifier: (type: DeviceBindingAuthenticationType) -> DeviceAuthenticator
        get() = {
            getDeviceAuthenticator(it)
        }

}