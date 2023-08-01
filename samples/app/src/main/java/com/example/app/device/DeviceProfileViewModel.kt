/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.device

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.forgerock.android.auth.FRDevice
import org.forgerock.android.auth.FRListener
import org.json.JSONObject

class DeviceProfileViewModel : ViewModel() {

    var state by mutableStateOf<JSONObject?>(null)
        private set

    init {
        getDeviceProfile()
    }

    fun getDeviceProfile() {
        FRDevice.getInstance().getProfile(object : FRListener<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                state = result
            }

            override fun onException(e: Exception) {
                state = null
            }

        })
    }

}