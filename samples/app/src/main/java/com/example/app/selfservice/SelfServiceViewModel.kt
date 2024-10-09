/*
 * Copyright (c) 2023 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.example.app.selfservice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.token.TokenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.forgerock.android.auth.selfservice.Device
import org.forgerock.android.auth.selfservice.UserRepository

class SelfServiceViewModel : ViewModel() {

    private val repo = UserRepository()
    private var selectedType = "Oath"

    var state = MutableStateFlow(SelfServiceState())
        private set

    fun delete(device: Device) {
        viewModelScope.launch {
            try {
                repo.delete(device)
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
                repo.update(device)
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
                    "Oath" -> repo.oathDevice()
                    "Push" -> repo.pushDevice()
                    "WebAuthn" -> repo.webAuthnDevice()
                    "Binding" -> repo.bindingDevice()
                    "Profile" -> repo.profileDevice()
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