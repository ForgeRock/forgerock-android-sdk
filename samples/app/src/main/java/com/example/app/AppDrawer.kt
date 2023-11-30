/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OnDeviceTraining
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.app.Destinations.CENTRALIZE_ROUTE
import com.example.app.Destinations.DEVICE_PROFILE
import com.example.app.Destinations.ENV_ROUTE
import com.example.app.Destinations.IG
import com.example.app.Destinations.LAUNCH_ROUTE
import com.example.app.Destinations.MANAGE_USER_KEYS
import com.example.app.Destinations.MANAGE_WEBAUTHN_KEYS
import com.example.app.Destinations.SETTING
import com.example.app.Destinations.TOKEN_ROUTE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    logoutViewModel: LogoutViewModel,
    navigateTo: (String) -> Unit,
    closeDrawer: () -> Unit) {

    val scroll = rememberScrollState(0)

    ModalDrawerSheet(
        modifier = Modifier
            .verticalScroll(scroll)) {
        Logo(
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Environment") },
            selected = false,
            icon = { Icon(Icons.Filled.ListAlt, null) },
            onClick = { navigateTo(ENV_ROUTE); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Launch Journey") },
            selected = false,
            icon = { Icon(Icons.Filled.RocketLaunch, null) },
            onClick = { navigateTo(LAUNCH_ROUTE); closeDrawer() },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Show Token") },
            selected = false,
            icon = { Icon(Icons.Filled.GeneratingTokens, null) },
            onClick = {
                navigateTo(TOKEN_ROUTE);
                closeDrawer()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Centralize Login") },
            selected = false,
            icon = { Icon(Icons.Filled.OpenInBrowser, null) },
            onClick = {
                navigateTo(CENTRALIZE_ROUTE);
                closeDrawer()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("WebAuthn Keys") },
            selected = false,
            icon = { Icon(Icons.Filled.Fingerprint, null) },
            onClick = {
                navigateTo(MANAGE_WEBAUTHN_KEYS);
                closeDrawer()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("User Keys") },
            selected = false,
            icon = { Icon(Icons.Filled.Key, null) },
            onClick = {
                navigateTo(MANAGE_USER_KEYS);
                closeDrawer()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("IG protected endpoint") },
            selected = false,
            icon = { Icon(Icons.Filled.Fence, null) },
            onClick = {
                navigateTo(IG);
                closeDrawer()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Device Profile") },
            selected = false,
            icon = { Icon(Icons.Filled.OnDeviceTraining, null) },
            onClick = {
                navigateTo(DEVICE_PROFILE);
                closeDrawer()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Setting") },
            selected = false,
            icon = { Icon(Icons.Filled.Settings, null) },
            onClick = {
                navigateTo(SETTING);
                closeDrawer()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Logout") },
            selected = false,
            icon = { Icon(Icons.Filled.Logout, null) },
            onClick = {
                logoutViewModel.logout();
                navigateTo(LAUNCH_ROUTE);
                closeDrawer()
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

@Composable
private fun Logo(modifier: Modifier) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .background(colorResource(id = R.color.black))
        .then(modifier)) {
        Icon(
            painterResource(R.drawable.ping_logo),
            contentDescription = null,
            modifier = Modifier
                .height(100.dp).padding(8.dp)
                .then(modifier),
            tint = Color.Unspecified,
        )
    }

}