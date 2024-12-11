/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.ig

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app.journey.Journey
import com.example.app.journey.JourneyViewModel
import org.forgerock.android.auth.PolicyAdvice
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IGRoute(viewModel: IGViewModel) {

    val igState by viewModel.state.collectAsState()

    var api by remember {
        mutableStateOf("https://openig.petrov.ca/products")
    }
    var checked by remember {
        mutableStateOf(true)
    }

    var showProgress by remember {
        mutableStateOf(false)
    }

    //Clean up the Continuation when dispose to avoid memory leak
    DisposableEffect(true) {
        onDispose {
            if (igState.transitionState is IGTransitionState.Authenticate) {
                if ((igState.transitionState as IGTransitionState.Authenticate).continuation.isActive) {
                    (igState.transitionState as IGTransitionState.Authenticate).continuation.cancel(
                        null)
                }
            }
        }
    }

    val scroll = rememberScrollState(0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopCenter)
    ) {
        Column(modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (showProgress) {
                CircularProgressIndicator()
            }
        }

        Column(modifier = Modifier.padding(8.dp)) {
            val context = LocalContext.current

            when (igState.transitionState) {
                is IGTransitionState.Start -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier
                        .fillMaxWidth()) {
                        OutlinedTextField(
                            modifier = Modifier,
                            value = api,
                            onValueChange = { value ->
                                api = value
                            },
                            label = { Text("Protected Endpoint URL") },
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { it ->
                                    checked = it
                                }
                            )
                            Text(
                                modifier = Modifier.padding(start = 2.dp),
                                text = "Use Header"
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            modifier = Modifier.align(Alignment.End),
                            onClick = {
                                showProgress = true
                                viewModel.invoke(context, api, checked)
                            }) {
                            Text("Continue")
                        }
                    }
                }

                is IGTransitionState.Authenticate -> {
                    showProgress = false
                    val state = igState.transitionState as IGTransitionState.Authenticate;
                    val journeyViewModel = viewModel<JourneyViewModel<PolicyAdvice>>(
                        factory = JourneyViewModel.factory(LocalContext.current,
                            state.policyAdvice))
                    Journey(journeyViewModel = journeyViewModel,
                        onSuccess = {
                            showProgress = true
                            if ((state).continuation.isActive) {
                                (state).continuation.resume(Unit) //Just to notify it is completed, it does not matter success or failed
                            }
                        },
                        onFailure = {
                            showProgress = true
                            if ((state).continuation.isActive) {
                                (state).continuation.resume(Unit) //Just to notify it is completed, it does not matter success or failed
                            }
                        }
                    )
                }

                is IGTransitionState.Finished -> {
                    showProgress = false
                    val state = igState.transitionState as IGTransitionState.Finished;
                    var result = ""
                    state.result?.let {
                        result = it
                    }
                    state.exception?.let {
                        result = it.message.toString()
                    }

                    Column(modifier = Modifier
                        .fillMaxWidth()) {
                        Card(
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 10.dp,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            border = BorderStroke(2.dp, Color.Black),
                            shape = MaterialTheme.shapes.medium) {
                            Text(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .verticalScroll(scroll),
                                text = result)
                        }
                    }
                }
            }
        }
    }
}
