/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.userkeys

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

class UserKeysViewModel(context: Context) : ViewModel() {

    private val frUserKeys = FRUserKeys(context)
    val userKeys = MutableStateFlow(UserKeysState())

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

    private fun fetch(t: Throwable?) {
        userKeys.update {
            it.copy(userKeys = frUserKeys.loadAll(), throwable = t)
        }
    }

    companion object {
        fun factory(
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserKeysViewModel(context.applicationContext) as T
            }
        }
    }
}