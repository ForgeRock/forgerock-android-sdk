/*
 * Copyright (c) 2023- 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContext = application as Application

       installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.isLoading.value
            }
        }

        setContent {
            CompositionLocalProvider(LocalAppContext provides appContext) {
                AuthApp()
            }
        }
    }
}

val LocalAppContext = compositionLocalOf<Application> {
    error("No Application context provided")
}

class MyApplication : Application()
