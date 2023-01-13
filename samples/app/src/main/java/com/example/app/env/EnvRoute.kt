/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.env

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.app.PreferenceViewModel
import com.example.app.Topbar
import org.forgerock.android.auth.FROptions

@Composable
fun EnvRoute(envViewModel: EnvViewModel, preferenceViewModel: PreferenceViewModel, openDrawer: () -> Unit) {
    val context = LocalContext.current
    envViewModel.select(context, preferenceViewModel.getLastEnv())
    EnvRoute(envViewModel.getAll(),
        envViewModel.current,
        openDrawer = openDrawer,
        onServerSelected = {
            envViewModel.select(context, it)
            preferenceViewModel.saveEnv(it.server.url)
        })
}


@Composable
fun EnvRoute(servers: List<FROptions>,
             current: FROptions,
             openDrawer: () -> Unit,
             onServerSelected: (FROptions) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Topbar(heading = "Select Environment", openDrawer)
        LazyColumn(modifier = Modifier) {
            servers.forEach {
                item {
                    ServerSetting(option = it, it == current, onServerSelected)
                }
            }
        }
    }
}

@Composable
private fun ServerSetting(option: FROptions,
                          selected: Boolean = false,
                          onServerSelected: (FROptions) -> Unit) {
    Column {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = option.server.url,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(16.dp))
            SelectServerButton(option, selected, onServerSelected)
        }
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun SelectServerButton(option: FROptions,
                               selected: Boolean,
                               onServerSelected: (FROptions) -> Unit) {
    val icon = if (selected) Icons.Filled.Done else Icons.Filled.CheckBoxOutlineBlank
    IconButton(
        onClick = { onServerSelected(option) }) {
        Icon(icon, contentDescription = null)
    }
}

