/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind


/**
 * Interface to be implemented by objects that want to be aware of [KeyAware] and [BiometricHandler]
 */
internal interface CryptoAware {

    fun setKeyAware(keyAware: KeyAware)
    fun setBiometricHandler(biometricHandler: BiometricHandler) {
        //do Nothing
    }

}