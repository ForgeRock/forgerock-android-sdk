/*
 * Copyright (c) 2023 - 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.forgerock.android.auth.AccessToken
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.FRUser

class TokenViewModel : ViewModel() {

    var state = MutableStateFlow(TokenState())
        private set

    init {
        getAccessToken()
    }

    fun getAccessToken() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                FRUser.getCurrentUser()?.accessToken?.let { token ->
                    state.update {
                        it.copy(token, null)
                    }
                }
            } catch (e: Exception) {
                yield()
                state.update {
                    it.copy(null, e)
                }
            }
        }
    }

    fun forceRefresh() {
        viewModelScope.launch(Dispatchers.Default) {
            FRUser.getCurrentUser()?.refresh(object : FRListener<AccessToken> {
                override fun onSuccess(result: AccessToken) {
                    state.update {
                        it.copy(result, null)
                    }
                }

                override fun onException(e: Exception) {
                    state.update {
                        it.copy(null, e)
                    }
                }
            })
        }
    }

    fun revoke(token: AccessToken? = FRUser.getCurrentUser()?.accessToken) {
        token?.let {
            FRUser.getCurrentUser()?.revokeAccessToken(object : FRListener<Void?> {
                override fun onSuccess(result: Void?) {
                    state.update {
                        it.copy(accessToken = null, exception = null)
                    }
                }

                override fun onException(e: Exception) {
                    state.update {
                        it.copy(accessToken = null, exception = e)
                    }
                }
            })
        }
    }


    fun setNullState() {
        state.update {
            it.copy(null, null)
        }
    }

}