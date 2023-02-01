/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback

@Composable
fun WebAuthnAuthenticationCallback(callback: WebAuthnAuthenticationCallback,
                                   node: Node,
                                   onCompleted: () -> Unit) {

    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        CircularProgressIndicator()
        LaunchedEffect(true) {
            launch {
                try {
                    callback.authenticate(context, node)
                    currentOnCompleted()
                } catch (e: CancellationException) {
                    //ignore
                } catch (e: Exception) {
                    currentOnCompleted()
                }
            }
        }
    }

}