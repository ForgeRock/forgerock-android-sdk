/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import org.forgerock.android.auth.devicebind.AuthenticatorFactory
import org.forgerock.android.auth.devicebind.BiometricBindingHandler
import org.forgerock.android.auth.devicebind.CryptoAware
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import org.forgerock.android.auth.devicebind.KeyAware
import java.util.*

interface Binding {

    /**
     * Inject crypto related objects to [DeviceAuthenticator]
     */
    fun initialize(userId: String,
                   title: String,
                   subtitle: String,
                   description: String,
                   deviceBindingAuthenticationType: DeviceBindingAuthenticationType,
                   deviceAuthenticator: DeviceAuthenticator) {
        //Inject objects
        if (deviceAuthenticator is CryptoAware) {
            deviceAuthenticator.setBiometricHandler(BiometricBindingHandler(title,
                subtitle,
                description,
                deviceBindAuthenticationType = deviceBindingAuthenticationType))
            deviceAuthenticator.setKeyAware(KeyAware(userId))
        }

    }

    /**
     * create the interface for the Authentication type(Biometric, Biometric_Fallback, none)
     */
    fun getDeviceBindAuthenticator(context: Context,
                                   deviceBindingAuthenticationType: DeviceBindingAuthenticationType): DeviceAuthenticator {
        return AuthenticatorFactory.getType(context, deviceBindingAuthenticationType)
    }

    /**
     * Get Expiration date for the signed token, claim "exp" will be set to the JWS.
     *
     * @return The expiration date
     */
    fun getExpiration(timeout: Int? ): Date {
        val date = Calendar.getInstance();
        date.add(Calendar.SECOND, timeout ?: 60)
        return date.time;
    }

}