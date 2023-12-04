/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.setting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.forgerock.android.auth.FRUserKeys
import org.forgerock.android.auth.devicebind.UserKey

class SettingViewModel(context: Context) : ViewModel() {

    private val frUserKeys = FRUserKeys(context)
    val settingState = MutableStateFlow(SettingState())

    init {
        fetch(null)
    }

    fun delete(userKey: UserKey) {
        val handler = CoroutineExceptionHandler { _, t ->
            fetch(t)
        }
        viewModelScope.launch(handler) {
            frUserKeys.delete(userKey, true)
            fetch(null)
        }
    }

    fun enable() {
        settingState.update {
            it.copy(transitionState = SettingTransitionState.EnableBinding)
        }
    }

    fun updateStateToEnable() {
        settingState.update {
            it.copy(transitionState = SettingTransitionState.Enabled)
        }
    }

    fun disable() {
        frUserKeys.loadAll().forEach {
            delete(it)
        }
        settingState.update {
            it.copy(transitionState = SettingTransitionState.Disabled)
        }
    }

    private fun fetch(t: Throwable?) {

        val state: SettingTransitionState = if (frUserKeys.loadAll().isNotEmpty()) {
            SettingTransitionState.Enabled
        } else {
            SettingTransitionState.Disabled
        }

        settingState.update {
            it.copy(transitionState = state)
        }
    }

    companion object {
        fun factory(
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingViewModel(context.applicationContext) as T
            }
        }
    }
}