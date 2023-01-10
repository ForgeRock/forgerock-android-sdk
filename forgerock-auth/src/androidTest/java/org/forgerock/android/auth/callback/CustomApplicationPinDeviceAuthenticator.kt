/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

import androidx.fragment.app.FragmentActivity
import org.forgerock.android.auth.devicebind.ApplicationPinDeviceAuthenticator

class CustomApplicationPinDeviceAuthenticator : ApplicationPinDeviceAuthenticator() {
    lateinit var pin: String

    override suspend fun requestForCredentials(fragmentActivity: FragmentActivity): CharArray {
        return pin.toCharArray()
    }
}