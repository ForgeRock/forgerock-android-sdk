/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.journey

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
class JourneyViewModel(context: Context, journeyName: String) : ViewModel() {

    val sharedPreferences = context.getSharedPreferences("JourneyPreferences", Context.MODE_PRIVATE)

    var state = MutableStateFlow(JourneyState())
        private set

    private val nodeListener = object : NodeListener<FRSession> {
        override fun onSuccess(result: FRSession) {
            state.update {
                it.copy(null, null, result)
            }
        }

        override fun onException(e: Exception) {
            state.update {
                //Not keep the node, so that we can retry with previous state
                it.copy(node = null, exception = e)
            }
        }

        override fun onCallbackReceived(node: Node) {
            state.update {
                it.copy(node = node, exception = null)
            }
        }
    }

    init {
        start(context, journeyName)
    }

    fun saveJourney(journeyName: String) {
        sharedPreferences.edit().putString("lastJourney", journeyName).apply()
    }

    fun getLastJourney(): String {
        return sharedPreferences.getString("lastJourney", "Login")!!
    }


    fun next(context: Context, node: Node) {
        viewModelScope.launch {
            node.next(context, nodeListener)
        }
    }

    private fun start(context: Context, journeyName: String) {
        viewModelScope.launch {
            FRSession.authenticate(context, journeyName, nodeListener)
        }
    }

    companion object {
        fun factory(
            context: Context,
            journeyName: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return JourneyViewModel(context.applicationContext, journeyName) as T
            }
        }
    }
}