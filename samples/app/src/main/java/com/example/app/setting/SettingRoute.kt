/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.setting

import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.journey.Journey
import com.example.app.journey.JourneyViewModel
import org.forgerock.android.auth.Action
import org.forgerock.android.auth.FRRequestInterceptor
import org.forgerock.android.auth.PolicyAdvice
import org.forgerock.android.auth.Request
import org.forgerock.android.auth.RequestInterceptorRegistry

private const val BINDING = "enableBiometric"

@Composable
fun SettingRoute(viewModel: SettingViewModel) {

    val checked by viewModel.settingState.collectAsState()

    when (checked.transitionState) {
        SettingTransitionState.Disabled -> {
            BiometricSetting(isChecked = false, viewModel = viewModel)
        }

        SettingTransitionState.EnableBinding -> {

            RequestInterceptorRegistry.getInstance()
                .register(object : FRRequestInterceptor<Action> {
                    override fun intercept(request: Request, tag: Action?): Request {
                        return if (tag?.payload?.getString("tree").equals(BINDING) ) {
                            request.newBuilder()
                                .url(Uri.parse(request.url().toString())
                                    .buildUpon()
                                    .appendQueryParameter("ForceAuth", "true").toString())
                                .build()
                        } else request
                    }
                })
            val journeyViewModel = viewModel<JourneyViewModel<PolicyAdvice>>(
                factory = JourneyViewModel.factory(LocalContext.current, BINDING))
            journeyViewModel.clear()
            journeyViewModel.start(LocalContext.current)
            Journey(journeyViewModel = journeyViewModel,
                onSuccess = {
                    viewModel.updateStateToEnable()
                },
                onFailure = {
                    viewModel.disable()
                }
            )
        }

        SettingTransitionState.Enabled -> {
            BiometricSetting(isChecked = true, viewModel = viewModel)
        }

        else -> {}
    }
}

@Composable
fun BiometricSetting(isChecked: Boolean, viewModel: SettingViewModel) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()) {
        Text(text = "Biometric Enable/Disable")
        Spacer(modifier = Modifier.weight(1f, true))
        Switch(
            checked = isChecked,
            onCheckedChange = {
                if (it) {
                    viewModel.enable();
                } else {
                    viewModel.disable();
                }
            }
        )
    }
}