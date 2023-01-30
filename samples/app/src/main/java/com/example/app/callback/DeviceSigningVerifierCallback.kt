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
import com.example.app.callback.binding.CustomAppPinDeviceAuthenticator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.forgerock.android.auth.callback.DeviceSigningVerifierCallback
import org.forgerock.android.auth.devicebind.ApplicationPinDeviceAuthenticator
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.Abort
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.Timeout
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.UnAuthorize
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.UnRegister
import org.forgerock.android.auth.devicebind.DeviceBindingException

@Composable
fun DeviceSigningVerifierCallback(callback: DeviceSigningVerifierCallback,
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
                run loop@{
                    //Client side retry
                    repeat(3) { //
                        try {
                            //Show how to use custom App Pin Dialog with Compose
                            callback.sign(context) { type ->
                                if (type == DeviceBindingAuthenticationType.APPLICATION_PIN) {
                                    CustomAppPinDeviceAuthenticator()
                                } else {
                                    callback.getDeviceAuthenticator(type)
                                }
                            }
                            currentOnCompleted()
                            return@loop
                        } catch (e: CancellationException) {
                            return@loop
                        } catch (e: DeviceBindingException) {
                            when (e.status) {
                                is UnRegister -> {
                                    callback.setClientError("UnReg")
                                }
                                is UnAuthorize -> {
                                    callback.setClientError("UnAuth")
                                }
                            }
                            if (it == 2 || e.status is Abort) {
                                currentOnCompleted()
                                return@loop
                            }
                        }
                    }
                }
            }
        }
    }

}