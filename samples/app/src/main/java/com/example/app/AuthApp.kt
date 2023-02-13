/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.app.theme.AppTheme
import kotlinx.coroutines.launch
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.callback.CallbackFactory
import org.forgerock.android.auth.callback.DeviceBindingCallback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthApp() {

    Logger.set(Logger.Level.DEBUG)

    AppTheme {
        val navController = rememberNavController()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val coroutineScope = rememberCoroutineScope()

        Surface(modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background) {

            ModalNavigationDrawer(
                drawerContent = {
                    val logoutViewModel = viewModel<LogoutViewModel>()
                    AppDrawer(
                        logoutViewModel = logoutViewModel,
                        navigateTo = { dest -> navController.navigate(dest) },
                        closeDrawer = { coroutineScope.launch { drawerState.close() } })
                },
                drawerState = drawerState,
                gesturesEnabled = true) {
                Box {
                    AppNavHost(navController = navController, openDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    })
                }
            }
        }
    }
}
