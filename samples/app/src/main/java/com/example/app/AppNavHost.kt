/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.app.centralize.Centralize
import com.example.app.centralize.CentralizeLoginViewModel
import com.example.app.journey.JourneyViewModel
import com.example.app.env.EnvRoute
import com.example.app.env.EnvViewModel
import com.example.app.journey.Journey
import com.example.app.journey.JourneyRoute
import com.example.app.token.Token
import com.example.app.token.TokenViewModel
import com.example.app.userkeys.UserKeysRoute
import com.example.app.userkeys.UserKeysViewModel
import com.example.app.webauthn.WebAuthRoute
import com.example.app.webauthn.WebAuthnViewModel

@Composable
fun AppNavHost(navController: NavHostController,
               startDestination: String = Destinations.ENV_ROUTE,
               openDrawer: () -> Unit) {

    NavHost(navController = navController,
        startDestination = startDestination) {

        composable(Destinations.ENV_ROUTE) {
            val envViewModel = viewModel<EnvViewModel>()
            val preferenceViewModel = viewModel<PreferenceViewModel>(
                factory = PreferenceViewModel.factory(LocalContext.current)
            )
            EnvRoute(envViewModel, preferenceViewModel, openDrawer)
        }

        composable(Destinations.LAUNCH_ROUTE) {
            val preferenceViewModel = viewModel<PreferenceViewModel>(
                factory = PreferenceViewModel.factory(LocalContext.current)
            )
            JourneyRoute(modifier = Modifier,
                preferenceViewModel = preferenceViewModel,
                openDrawer = openDrawer,
                onSubmit = { journeyName ->
                    navController.navigate("$Destinations.JOURNEY_ROUTE/$journeyName")
                })
        }
        composable(Destinations.TOKEN_ROUTE) {
            val tokenViewModel = viewModel<TokenViewModel>()
            Token(tokenViewModel, openDrawer)
        }
        composable(Destinations.CENTRALIZE_ROUTE) {
            val centralizeLoginViewModel = viewModel<CentralizeLoginViewModel>()
            Centralize(centralizeLoginViewModel, openDrawer)
        }
        composable(Destinations.MANAGE_WEBAUTHN_KEYS) {
            val webAuthnViewModel = viewModel<WebAuthnViewModel>(
                factory = WebAuthnViewModel.factory(LocalContext.current)
            )
            WebAuthRoute(webAuthnViewModel, openDrawer)
        }
        composable(Destinations.MANAGE_USER_KEYS) {
            val userKeysViewModel = viewModel<UserKeysViewModel>(
                factory = UserKeysViewModel.factory(LocalContext.current)
            )
            UserKeysRoute(userKeysViewModel, openDrawer)
        }
        composable("$Destinations.JOURNEY_ROUTE/{name}", arguments = listOf(
            navArgument("name") { type = NavType.StringType }
        )) {
            it.arguments?.getString("name")?.apply {
                val journeyViewModel = viewModel<JourneyViewModel>(
                    factory = JourneyViewModel.factory(LocalContext.current, this)
                )
                Journey(this, journeyViewModel, openDrawer)
            }
        }

    }

}