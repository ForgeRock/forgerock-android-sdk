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
import org.forgerock.android.auth.AccessToken
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.FRUser
import java.lang.Exception

class TokenViewModel : ViewModel() {

    var state by mutableStateOf<AccessToken?>(null)
        private set

    init {
        getAccessToken()
    }

    fun getAccessToken() {
        FRUser.getCurrentUser()?.getAccessToken(object : FRListener<AccessToken> {
            override fun onSuccess(result: AccessToken) {
                state = result
            }

            override fun onException(e: Exception) {
                state = null
            }
        })
    }

}