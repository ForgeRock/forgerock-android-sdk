/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.userprofile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.FRUser
import org.forgerock.android.auth.UserInfo
import org.json.JSONObject

class UserProfileViewModel : ViewModel() {

    var state = MutableStateFlow(UserProfileState())
        private set


    init {
        showSession()
    }

    private fun showSession() {
        val jsonObject = JSONObject()
        jsonObject.put("SessionToken",
            FRSession.getCurrentSession()?.sessionToken?.value)
        state.update {
            it.copy(user = jsonObject, exception = null)
        }
    }

    fun userinfo() {
        FRUser.getCurrentUser()?.getUserInfo(object : FRListener<UserInfo> {
            override fun onSuccess(result: UserInfo) {
                state.update {
                    it.copy(user = result.raw, exception = null)
                }
            }

            override fun onException(e: Exception) {
                state.update {
                   it.copy(user = null, exception = e)
                }
            }
        })
    }

}