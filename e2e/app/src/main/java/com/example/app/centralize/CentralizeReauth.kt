/*
 * Copyright (c) 2023- 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.centralize

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.app.Error
import com.example.app.callback.NameCallback as NameCallbackView
import com.example.app.callback.PasswordCallback as PasswordCallbackView
import org.forgerock.android.auth.callback.NameCallback
import org.forgerock.android.auth.callback.PasswordCallback

@Composable
fun CentralizeReauth(centralizeReauthViewModel: CentralizeReauthViewModel) {

    val context = LocalContext.current
    val state by centralizeReauthViewModel.state.collectAsState()

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()) {

        Text("Central Login + re-authentication Journey (SDKS-3046 manual harness)")
        Text("Central login completed. Run a Journey with forceAuth — before the fix this " +
                "revoked the centralized-login OAuth2.0 tokens; after the fix it must not.")

        Row(modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()) {
            Button(onClick = { centralizeReauthViewModel.useCase1(context) }) {
                Text("Use Case 1: forceAuth=true")
            }
            Button(modifier = Modifier.padding(start = 8.dp),
                onClick = { centralizeReauthViewModel.useCase2(context) }) {
                Text("Use Case 2: +noSession")
            }
        }

        //Session/token state — both must stay true after a Use Case run ("no Session or
        //OAuth2.0 tokens was revoked").
        Text("SSO session present: ${state.sessionTokenPresent}")
        Text("OAuth2.0 tokens present: ${state.oauth2TokenPresent}")

        state.exception?.apply {
            Error(exception = this)
        }

        state.node?.apply {
            var showNext = true
            callbacks?.forEach {
                when (it) {
                    is NameCallback -> NameCallbackView(it)
                    is PasswordCallback -> PasswordCallbackView(it)
                    else -> {}
                }
                //TextOutputCallback -> TextOutputCallback(it)  // add more callback types here if the Journey needs them
            }
            if (showNext) {
                Button(modifier = Modifier.align(Alignment.End), onClick = {
                    centralizeReauthViewModel.next(context, state.node!!)
                }) {
                    Text("Next")
                }
            }
        }

        state.session?.apply {
            Text("Journey completed — session established.")
        }
    }
}
