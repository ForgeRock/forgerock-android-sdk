/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.userkeys

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.app.Alert
import com.example.app.Topbar
import org.forgerock.android.auth.devicebind.UserKey

@Composable
fun UserKeysRoute(viewModel: UserKeysViewModel, openDrawer: () -> Unit) {

    val userKeysState by viewModel.userKeys.collectAsState()

    var showConfirmation by remember {
        mutableStateOf(false)
    }

    var state by remember {
        mutableStateOf(UserKeyState())
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            confirmButton = {
                TextButton(onClick = {
                    state.userKey?.let {
                        viewModel.delete(it)
                    }
                    showConfirmation = false
                })
                { Text(text = "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false })
                { Text(text = "Cancel") }
            },
            text = {
                state.userKey?.let { Text(text = "${it.userName} - ${it.authType.name}") }
            }
        )
    }

    userKeysState.throwable?.apply {
        Alert(throwable = this)
        userKeysState.throwable = null
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Topbar(heading = "User Keys - Device Binding", openDrawer)
        UserKeys(userKeysState.userKeys,
            onSelected = {
                showConfirmation = true
                state = UserKeyState(it)
            })
    }
}

@Composable
fun UserKeys(userKeys: List<UserKey>,
             onSelected: (UserKey) -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        LazyColumn(modifier = Modifier) {
            userKeys.forEach {
                item {
                    UserKey(userKey = it, onSelected)
                }
            }
        }
    }
}

@Composable
private fun UserKey(userKey: UserKey,
                   onSelected: (UserKey) -> Unit) {
    Column {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${userKey.userName} - ${userKey.authType.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(userKey) })
        }
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}
