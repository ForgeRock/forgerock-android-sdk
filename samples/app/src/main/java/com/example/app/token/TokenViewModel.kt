/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.token

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.app.userprofile.UserProfileState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.forgerock.android.auth.AccessToken
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.FRUser
import java.lang.Exception

class TokenViewModel : ViewModel() {

    var state = MutableStateFlow(TokenState())
        private set

    init {
        getAccessToken()
    }

    fun getAccessToken() {
        FRUser.getCurrentUser()?.getAccessToken(object : FRListener<AccessToken> {
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

    fun forceRefresh(token: AccessToken? = FRUser.getCurrentUser()?.accessToken) {
        token?.let {
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

    fun setNullState() {
        state.update {
            it.copy(null, null)
        }
    }

}