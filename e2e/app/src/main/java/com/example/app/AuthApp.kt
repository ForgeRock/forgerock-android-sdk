/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.app.theme.AppTheme
import kotlinx.coroutines.launch
import org.forgerock.android.auth.Logger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthApp() {

    Logger.set(Logger.Level.DEBUG)

    AppTheme {
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val coroutineScope = rememberCoroutineScope()

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentScreen = getTitle(backStackEntry)

        Surface(modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background) {
            val logoutViewModel = viewModel<LogoutViewModel>()

            ModalNavigationDrawer(
                drawerContent = {
                   AppDrawer(
                        logoutViewModel = logoutViewModel,
                        navigateTo = { dest -> navController.navigate(dest) },
                        closeDrawer = { coroutineScope.launch { drawerState.close() } })
                },
                drawerState = drawerState,
                gesturesEnabled = true) {
                Scaffold(
                    topBar = {
                        // to run the animation independently
                        TopAppBar(
                            title = { Text(currentScreen) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        // opens drawer
                                        drawerState.open()
                                    }
                                }) {
                                    Icon(Icons.Rounded.Menu,
                                        contentDescription = "MenuButton")
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    logoutViewModel.logout()
                                    navController.navigate(Destinations.LAUNCH_ROUTE)
                                }) {
                                    Icon(Icons.Filled.Logout, "Logout")
                                }
                            }
                        )
                    }
                ) { values ->
                    Surface(Modifier
                        .fillMaxSize()
                        .padding(values)) {
                        AppNavHost(navController = navController)
                    }
                }
            }
        }
    }
}

fun getTitle(backStackEntry: NavBackStackEntry?): String {
    var result = backStackEntry?.destination?.route ?: Destinations.ENV_ROUTE
    if (result.startsWith(Destinations.JOURNEY_ROUTE)) {
        result =
            Destinations.JOURNEY_ROUTE + "- ${backStackEntry?.arguments?.getString("name")}"
    }
    return result

}
