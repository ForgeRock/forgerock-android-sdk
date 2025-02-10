/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.journey.JourneyState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.NodeListener
import java.lang.Exception

/**
 * Should avoid passing context to ViewModel
 */
class PreferenceViewModel(context: Context) : ViewModel() {

    private val sharedPreferences = context.getSharedPreferences("JourneyPreferences", Context.MODE_PRIVATE)

    fun saveJourney(journeyName: String) {
        sharedPreferences.edit().putString("lastJourney", journeyName).apply()
    }

    fun getLastJourney(): String {
        return sharedPreferences.getString("lastJourney", "Login")!!
    }

    fun saveEnv(envName: String) {
        sharedPreferences.edit().putString("env", envName).apply()
    }

    fun getLastEnv() : String {
        return sharedPreferences.getString("env", "localhost")!!
    }

    companion object {
        fun factory(
            context: Context,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PreferenceViewModel(context.applicationContext) as T
            }
        }
    }
}