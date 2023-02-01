/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.webauthn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.app.Topbar
import org.forgerock.android.auth.webauthn.PublicKeyCredentialSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebAuthRoute(webAuthnViewModel: WebAuthnViewModel, openDrawer: () -> Unit) {

    val sources by webAuthnViewModel.sources.collectAsState()

    var showConfirmation by remember {
        mutableStateOf(false)
    }

    var state by remember {
        mutableStateOf(WebAuthnState())
    }

    var rpid by remember {
        mutableStateOf("")
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            confirmButton = {
                TextButton(onClick = {
                    state.publicKeyCredentialSource?.let {
                        webAuthnViewModel.delete(it)
                    }
                    webAuthnViewModel.fetch(rpid)
                    showConfirmation = false
                })
                { Text(text = "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false })
                { Text(text = "Cancel") }
            },
            text = {
                state.publicKeyCredentialSource?.toJson()?.let { Text(text = it.toString(4)) }
            }

        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Topbar(heading = "WebAuthn Keys", openDrawer)
        Row(modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()) {
            OutlinedTextField(
                modifier = Modifier,
                value = rpid,
                onValueChange = { value ->
                    rpid = value
                    webAuthnViewModel.fetch(rpid)
                },
                label = { Text("Relying Party Id") },
            )
        }
        WebAuthnRoute(sources,
            onSelected = {
                showConfirmation = true
                state = WebAuthnState(it)
            })
        if (sources.isNotEmpty()) {
            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = { webAuthnViewModel.delete(rpid) }) {
                Text(text = "Delete All")
            }
        }
    }
}

@Composable
fun WebAuthnRoute(sources: List<PublicKeyCredentialSource>,
                  onSelected: (PublicKeyCredentialSource) -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        LazyColumn(modifier = Modifier) {
            sources.forEach {
                item {
                    Source(source = it, onSelected)
                }
            }
        }
    }
}

@Composable
private fun Source(source: PublicKeyCredentialSource,
                   onSelected: (PublicKeyCredentialSource) -> Unit) {
    Column {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = source.otherUI,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(source) })
        }
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}
