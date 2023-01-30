/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.app.Error
import org.forgerock.android.auth.callback.TextOutputCallback
import org.forgerock.android.auth.callback.TextOutputCallback.ERROR
import org.forgerock.android.auth.callback.TextOutputCallback.INFORMATION
import org.forgerock.android.auth.callback.TextOutputCallback.WARNING
import java.lang.NullPointerException

@Composable
fun TextOutputCallback(callback: TextOutputCallback) {

    Row(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()) {
        when (callback.messageType) {
            INFORMATION -> Icon(Icons.Filled.Info, null)
            WARNING -> Icon(Icons.Filled.Warning, null)
            ERROR -> Icon(Icons.Filled.Error, null)
            else -> Icon(Icons.Filled.Info, null)
        }
        Spacer(Modifier.width(8.dp))
        Text(text = callback.message,
            Modifier
                .weight(1f),
            style = MaterialTheme.typography.titleMedium
        )
    }

}