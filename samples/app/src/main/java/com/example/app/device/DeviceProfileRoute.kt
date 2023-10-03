/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.device

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable
fun DeviceProfileRoute(deviceProfileViewModel: DeviceProfileViewModel) {

    val result: JSONObject? = deviceProfileViewModel.state
    val scroll = rememberScrollState(0)

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()
    ) {
        Card(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 10.dp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            border = BorderStroke(2.dp, Color.Black),
            shape = MaterialTheme.shapes.medium) {
            Text(
                modifier = Modifier
                    .padding(4.dp)
                    .verticalScroll(scroll),
                text = result?.toString(4)
                    ?: "")
        }
    }
}

