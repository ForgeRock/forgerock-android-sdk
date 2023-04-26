/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.userprofile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.app.Alert
import com.example.app.Topbar

@Composable
fun UserProfile(userProfileViewModel: UserProfileViewModel, openDrawer: () -> Unit) {

    val state by userProfileViewModel.state.collectAsState()

    Column(modifier = Modifier
        .fillMaxWidth()) {
        Topbar(heading = "UserInfo", openDrawer = openDrawer)
        state.user?.apply {
            val userinfo = this
            Card(
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 10.dp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                border = BorderStroke(2.dp, Color.Black),
                shape = MaterialTheme.shapes.medium) {
                Text(
                    modifier = Modifier.padding(4.dp),
                    text = userinfo.toString(4) ?: "")
            }
        }
        state.exception?.apply {
            Alert(throwable = this)
        }

        Button(
            onClick = { userProfileViewModel.userinfo() }) {
            Text(text = "Show UserInfo")
        }
    }
}

