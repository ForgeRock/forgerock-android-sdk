/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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

}