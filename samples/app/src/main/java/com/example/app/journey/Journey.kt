/*
 * Copyright (c) 2023 - 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.journey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.app.Error
import com.example.app.callback.AppIntegrityCallback
import com.example.app.callback.ChoiceCallback
import com.example.app.callback.ConfirmationCallback
import com.example.app.callback.DeviceBindingCallback
import com.example.app.callback.DeviceProfileCallback
import com.example.app.callback.DeviceSigningVerifierCallback
import com.example.app.callback.IdPCallback
import com.example.app.callback.NameCallback
import com.example.app.callback.PasswordCallback
import com.example.app.callback.PingOneProtectEvaluationCallback
import com.example.app.callback.PingOneProtectInitializeCallback
import com.example.app.callback.PollingWaitCallback
import com.example.app.callback.SelectIdPCallback
import com.example.app.callback.TextInputCallback
import com.example.app.callback.TextOutputCallback
import com.example.app.callback.WebAuthnAuthenticationCallback
import com.example.app.callback.WebAuthnRegistrationCallback
import org.forgerock.android.auth.callback.AppIntegrityCallback
import org.forgerock.android.auth.callback.ChoiceCallback
import org.forgerock.android.auth.callback.ConfirmationCallback
import org.forgerock.android.auth.callback.DeviceBindingCallback
import org.forgerock.android.auth.callback.DeviceProfileCallback
import org.forgerock.android.auth.callback.DeviceSigningVerifierCallback
import org.forgerock.android.auth.callback.IdPCallback
import org.forgerock.android.auth.callback.NameCallback
import org.forgerock.android.auth.callback.PasswordCallback
import org.forgerock.android.auth.callback.PollingWaitCallback
import org.forgerock.android.auth.callback.ReCaptchaCallback
import com.example.app.callback.ReCaptchaCallback
import org.forgerock.android.auth.PingOneProtectEvaluationCallback
import org.forgerock.android.auth.PingOneProtectInitializeCallback
import org.forgerock.android.auth.callback.SelectIdPCallback
import org.forgerock.android.auth.callback.TextInputCallback
import org.forgerock.android.auth.callback.TextOutputCallback
import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback

@Composable
fun <T> Journey(journeyViewModel: JourneyViewModel<T>,
                onSuccess: (() -> Unit)? = null,
                onFailure: ((Exception) -> Unit)? = null) {

    val context = LocalContext.current
    val state by journeyViewModel.state.collectAsState()
    val currentOnSuccess by rememberUpdatedState(onSuccess)
    val currentOnFailure by rememberUpdatedState(onFailure)

    Journey(state = state,
        onNext = { state.node?.let { journeyViewModel.next(context, it) } },
        currentOnSuccess, currentOnFailure)

}

@Composable
fun Journey(state: JourneyState,
            onNext: () -> Unit,
            onSuccess: (() -> Unit)?,
            onFailure: ((Exception) -> Unit)?) {

    Column(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()) {
        state.session?.apply {
            LaunchedEffect(true) {
                onSuccess?.let { onSuccess() }
            }
        }
        state.exception?.apply {
            val exception = this
            Error(exception)
            LaunchedEffect(true) {
                onFailure?.let { onFailure(exception) }
            }
        }
        state.node?.apply {
            var showNext = true
            state.node.callbacks?.forEach {
                when (it) {
                    is NameCallback -> NameCallback(it)
                    is PasswordCallback -> PasswordCallback(it)
                    is DeviceBindingCallback -> {
                        DeviceBindingCallback(it, onCompleted = onNext)
                        showNext = false
                    }

                    is TextOutputCallback -> TextOutputCallback(it)
                    is DeviceSigningVerifierCallback -> {
                        DeviceSigningVerifierCallback(it, true, onCompleted = onNext)
                        showNext = false
                    }

                    is ConfirmationCallback -> {
                        ConfirmationCallback(it, onSelected = onNext)
                        showNext = false
                    }

                    is WebAuthnRegistrationCallback -> {
                        WebAuthnRegistrationCallback(it, state.node, onCompleted = onNext)
                        showNext = false
                    }

                    is WebAuthnAuthenticationCallback -> {
                        WebAuthnAuthenticationCallback(it, state.node, onCompleted = onNext)
                        showNext = false
                    }

                    is ChoiceCallback -> {
                        ChoiceCallback(it)
                    }

                    is PollingWaitCallback -> {
                        PollingWaitCallback(it, onTimeout = onNext)
                    }

                    is SelectIdPCallback -> {
                        SelectIdPCallback(callback = it, onSelected = onNext)
                    }
                    is ReCaptchaCallback -> {
                        ReCaptchaCallback(it, state.node, onCompleted = onNext)
                        showNext = false
                    }
                    is AppIntegrityCallback -> {
                        AppIntegrityCallback(callback = it, onCompleted = onNext)
                        showNext = false
                    }
                    is DeviceProfileCallback -> {
                        DeviceProfileCallback(callback = it, onCompleted = onNext)
                        showNext = false
                    }

                    is IdPCallback -> {
                        IdPCallback(callback = it, onCompleted = onNext)
                        showNext = false
                    }

                    is PingOneProtectEvaluationCallback -> {
                        PingOneProtectEvaluationCallback(callback = it, onCompleted = onNext)
                        showNext = false
                    }

                    is PingOneProtectInitializeCallback -> {
                        PingOneProtectInitializeCallback(callback = it, onCompleted = onNext)
                        showNext = false
                    }

                    is TextInputCallback -> TextInputCallback(it)


                    else -> {
                        //Unsupported
                    }
                }
            }
            if (showNext) {
                Button(modifier = Modifier.align(Alignment.End),
                    onClick = onNext) {
                    Text("Next")
                }
            }
        }
    }

}

