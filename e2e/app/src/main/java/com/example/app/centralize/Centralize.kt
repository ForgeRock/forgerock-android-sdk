/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.centralize

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.Error
import com.example.app.userprofile.UserProfile
import com.example.app.userprofile.UserProfileViewModel

@Composable
fun Centralize(centralizeLoginViewModel: CentralizeLoginViewModel) {

    val activity = LocalContext.current as FragmentActivity

    LaunchedEffect(true) {
        //Not relaunch when recomposition
        centralizeLoginViewModel.login(activity)
    }

    val state by centralizeLoginViewModel.state.collectAsState()


    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()) {
        state.user?.apply {
            val userProfileViewModel =
                viewModel<UserProfileViewModel>()
            UserProfile(userProfileViewModel = userProfileViewModel)
        }
        state.exception?.apply {
            Error(exception = this)
        }
    }
}

