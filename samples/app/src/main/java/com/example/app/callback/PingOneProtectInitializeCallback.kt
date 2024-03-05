/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.PingOneProtectInitializeCallback
import org.forgerock.android.auth.PingOneProtectInitException

@Composable
fun PingOneProtectInitializeCallback(
    callback: PingOneProtectInitializeCallback,
    onCompleted: () -> Unit,
) {
    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val context = LocalContext.current

    Column(
        modifier =
        Modifier
            .padding(8.dp)
            .fillMaxHeight()
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Collecting PingOne Signals...")
        Spacer(Modifier.height(8.dp))
        CircularProgressIndicator()

        LaunchedEffect(true) {
            try {
                callback.start(context)
            } catch (e: PingOneProtectInitException) {
                Logger.error("PingOneInitException", e, e.message)
            } catch (e: Exception) {
                Logger.error("PingOneInitException", e, e.message)
            }
            currentOnCompleted()
        }
    }
}
