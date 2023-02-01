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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.forgerock.android.auth.FRUserKeys
import org.forgerock.android.auth.devicebind.UserKey

class UserKeysViewModel(context: Context) : ViewModel() {

    private val frUserKeys = FRUserKeys(context)

    val userKeys = MutableStateFlow(emptyList<UserKey>())

    init {
        fetch()
    }

    fun delete(userKey: UserKey) {
        frUserKeys.delete(userKey)
        fetch()
    }

    fun fetch() {
        userKeys.update {
            frUserKeys.loadAll()
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