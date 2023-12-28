/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.app.callback.binding.CustomAppPinDeviceAuthenticator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.forgerock.android.auth.callback.DeviceSigningVerifierCallback
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus
import org.forgerock.android.auth.devicebind.DeviceBindingException

@Composable
fun DeviceSigningVerifierCallback(callback: DeviceSigningVerifierCallback,
                                  showChallenge: Boolean = false,
                                  onCompleted: () -> Unit) {

    //We should not put input callback function (onCompleted) to coroutineScope or LaunchEffect
    //onCompleted may call after LaunchEffect finished
    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scroll = rememberScrollState(0)
    var showProgress by remember {
        mutableStateOf(true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        Column(modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (showProgress) {
                CircularProgressIndicator()
            }
        }
        if (showChallenge) {
            showProgress = false
            Column(modifier = Modifier
                .fillMaxWidth()) {
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    border = BorderStroke(2.dp, Color.Black),
                    shape = MaterialTheme.shapes.medium) {
                    Text(modifier = Modifier
                        .padding(4.dp)
                        .verticalScroll(scroll),
                        text = callback.challenge)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = {
                        showProgress = true
                        coroutineScope.launch {
                            try {
                                sign(context, callback)
                                currentOnCompleted()
                            } catch (e: CancellationException) {
                                //ignore
                            } catch (e: Exception) {
                                currentOnCompleted()
                            }
                        }
                    }) {
                    Text("Approve")
                }
            }
        } else {
            LaunchedEffect(true) {
                try {
                    sign(context, callback)
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

suspend fun sign(context: Context, callback: DeviceSigningVerifierCallback) {
    run loop@{
        //Client side retry example
        repeat(3) { //
            try {
                //Show how to use custom App Pin Dialog with Compose
                callback.sign(context, mapOf("os" to "android")) { type ->
                    if (type == DeviceBindingAuthenticationType.APPLICATION_PIN) {
                        CustomAppPinDeviceAuthenticator()
                    } else {
                        callback.getDeviceAuthenticator(type)
                    }
                }
                return@loop
            } catch (e: DeviceBindingException) {
                when (e.status) {
                    is DeviceBindingErrorStatus.UnAuthorize -> {
                        //custom error example
                        //callback.setClientError("UnAuth")
                    }
                    is DeviceBindingErrorStatus.Abort -> {
                        return@loop
                    }
                    is DeviceBindingErrorStatus.ClientNotRegistered -> {
                        return@loop
                    }
                }
                //Retry 3 times already
                if (it == 2) {
                    return@loop
                }
            }
        }
    }
}