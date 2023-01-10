/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

import org.json.JSONObject

class CustomApplicationPinDeviceBindingCallback : DeviceBindingCallback {
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)
    constructor() : super()

    val deviceAuthenticator: CustomApplicationPinDeviceAuthenticator =
        CustomApplicationPinDeviceAuthenticator()

    override fun getDeviceAuthenticator(type: DeviceBindingAuthenticationType): CustomApplicationPinDeviceAuthenticator {
        return deviceAuthenticator
    }
}