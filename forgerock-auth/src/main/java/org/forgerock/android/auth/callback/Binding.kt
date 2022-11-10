/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import java.util.*

/**
 * Device Binding interface to provide utility method for [DeviceBindingCallback] and [DeviceSigningVerifierCallback]
 */
interface Binding {

    /**
     * Create the interface for the Authentication type(Biometric, Biometric_Fallback, none)
     * @param context The Application Context
     * @param type The Device Binding Authentication Type
     * @return The recommended  [DeviceAuthenticator] that can handle the provide [DeviceBindingAuthenticationType]
     */
    fun getDeviceAuthenticator(context: Context,
                               type: DeviceBindingAuthenticationType): DeviceAuthenticator =
        type.getAuthType(context)

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

}