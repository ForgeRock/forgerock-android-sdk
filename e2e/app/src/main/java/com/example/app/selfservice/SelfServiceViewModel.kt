/*
 * Copyright (c) 2023 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.example.app.selfservice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.forgerock.android.auth.selfservice.BoundDevice
import org.forgerock.android.auth.selfservice.Device
import org.forgerock.android.auth.selfservice.DeviceClient
import org.forgerock.android.auth.selfservice.OathDevice
import org.forgerock.android.auth.selfservice.ProfileDevice
import org.forgerock.android.auth.selfservice.PushDevice
import org.forgerock.android.auth.selfservice.WebAuthnDevice

class SelfServiceViewModel : ViewModel() {

    private val repo = DeviceClient()
    private var selectedType = "Oath"

    var state = MutableStateFlow(SelfServiceState())
        private set

    fun delete(device: Device) {
        viewModelScope.launch {
            try {
                when (device) {
                    is OathDevice -> repo.oath.delete(device)
                    is PushDevice -> repo.push.delete(device)
                    is WebAuthnDevice -> repo.webAuthn.delete(device)
                    is BoundDevice -> repo.bound.delete(device)
                    is ProfileDevice -> repo.profile.delete(device)
                    else -> throw IllegalArgumentException("Unsupported Device Type")
                }
                fetch(selectedType)
            } catch (e: Exception) {
                yield()
                state.update { it.copy(devices = emptyList(), throwable = e) }

            }
        }
    }

    fun update(device: Device) {
        viewModelScope.launch {
            try {
                when (device) {
                    is WebAuthnDevice -> repo.webAuthn.update(device)
                    is BoundDevice -> repo.bound.update(device)
                    is ProfileDevice -> repo.profile.update(device)
                    else -> throw IllegalArgumentException("Unsupported Device Type")
                }
                fetch(selectedType)
            } catch (e: Exception) {
                yield()
                state.update { it.copy(devices = emptyList(), throwable = e) }
            }

        }
    }

    fun fetch(type: String) {
        var selectedDevices: List<Device>
        selectedType = type
        viewModelScope.launch {
            try {
                selectedDevices = when (type) {
                    "Oath" -> repo.oath.get()
                    "Push" -> repo.push.get()
                    "WebAuthn" -> repo.webAuthn.get()
                    "Binding" -> repo.bound.get()
                    "Profile" -> repo.profile.get()
                    else -> emptyList()
                }
                state.update { it.copy(devices = selectedDevices, throwable = null) }
            } catch (e: Exception) {
                yield()
                state.update { it.copy(devices = emptyList(), throwable = e) }
            }
        }
    }

    fun clear() {
        state.value = SelfServiceState()
    }
}