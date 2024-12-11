/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
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
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.callback.IdPCallback

@Composable
fun IdPCallback(callback: IdPCallback,
                onCompleted: () -> Unit) {
    val currentOnCompleted by rememberUpdatedState(onCompleted)

    Column(modifier = Modifier
        .padding(8.dp)
        .fillMaxHeight()
        .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Launching ${callback.provider} Social Login...")
        Spacer(Modifier.height(8.dp))
        CircularProgressIndicator()

        LaunchedEffect(true) {
            callback.signIn(null, object: FRListener<Void?> {
                override fun onSuccess(result: Void?) {
                    currentOnCompleted()
                }
                override fun onException(e: Exception) {
                    currentOnCompleted()
                }
            })
        }
    }
}