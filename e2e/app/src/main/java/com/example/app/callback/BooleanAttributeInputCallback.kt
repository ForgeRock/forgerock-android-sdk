/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.forgerock.android.auth.callback.BooleanAttributeInputCallback

@Composable
fun BooleanAttributeInputCallback(callback: BooleanAttributeInputCallback) {

    var input by remember {
        mutableStateOf(callback.value)
    }

    Row(modifier = Modifier
        .padding(4.dp)
        .fillMaxWidth()) {
        Text(text = callback.prompt)
        Spacer(modifier = Modifier.weight(1f, true))
        Switch(
            checked = input,
            onCheckedChange = {
                input = it
                callback.value = it
            }
        )
    }

}