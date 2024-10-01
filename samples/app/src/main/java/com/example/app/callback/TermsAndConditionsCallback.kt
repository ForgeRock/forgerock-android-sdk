/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.forgerock.android.auth.callback.TermsAndConditionsCallback

@Composable
fun TermsAndConditionsCallback(callback: TermsAndConditionsCallback) {

    var input by remember {
        mutableStateOf(false)
    }

    Row(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()) {
        Text(text = callback.version,
            Modifier
                .weight(1f),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.width(8.dp))
        Text(text = callback.createDate,
            Modifier
                .weight(1f),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.width(8.dp))
        Text(text = callback.terms,
            Modifier
                .weight(1f),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = input,
            onCheckedChange = {
                input = it
                callback.setAccept(it)
            }
        )
    }

}