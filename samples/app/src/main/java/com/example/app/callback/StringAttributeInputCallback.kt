/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.forgerock.android.auth.callback.StringAttributeInputCallback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StringAttributeInputCallback(callback: StringAttributeInputCallback) {

    var input by remember {
        mutableStateOf(callback.value)
    }

    Row(modifier = Modifier
        .padding(4.dp)
        .fillMaxWidth()) {
        OutlinedTextField(
            modifier = Modifier,
            value = input,
            onValueChange = { value ->
                input = value
                callback.value = input
            },
            isError = callback.hasError(),
            supportingText = if (callback.hasError()) {
                @Composable {
                    Text(
                        text = callback.error(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else null,
            label = { Text(callback.prompt) },
        )
    }
}