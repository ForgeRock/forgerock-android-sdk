/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.app.Destinations.CENTRALIZE_ROUTE
import com.example.app.Destinations.ENV_ROUTE
import com.example.app.Destinations.LAUNCH_ROUTE
import com.example.app.Destinations.MANAGE_USER_KEYS
import com.example.app.Destinations.MANAGE_WEBAUTHN_KEYS
import com.example.app.Destinations.TOKEN_ROUTE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    logoutViewModel: LogoutViewModel,
    navigateTo: (String) -> Unit,
    closeDrawer: () -> Unit) {

    ModalDrawerSheet {
        Logo(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 48.dp)
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
private fun Logo(modifier: Modifier = Modifier) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.primary)
        .then(modifier)) {
        Icon(
            painterResource(R.drawable.forgerock),
            contentDescription = null
        )
        Spacer(Modifier.width(8.dp))
        Text(text = "ForgeRock")
    }

}