/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.forgerock.android.auth.callback.PollingWaitCallback

@Composable
fun PollingWaitCallback(callback: PollingWaitCallback, onTimeout: () -> Unit) {

    val currentOnTimeout by rememberUpdatedState(onTimeout)
    val infiniteTransition = rememberInfiniteTransition()
    val duration = callback.waitTime.toInt()

    val progressAnimationValue by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(duration)))


    Column(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = callback.message)
        Spacer(Modifier.height(8.dp))
        CircularProgressIndicator(progress = progressAnimationValue)

        LaunchedEffect(callback) {
            delay(callback.waitTime.toLong())
            currentOnTimeout()
        }
    }

}

