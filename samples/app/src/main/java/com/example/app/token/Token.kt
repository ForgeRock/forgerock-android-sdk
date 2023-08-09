/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.token

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.app.Alert
import org.json.JSONObject

@Composable
fun Token(tokenViewModel: TokenViewModel) {

    val tokenState by tokenViewModel.state.collectAsState()
    val scroll = rememberScrollState(0)

    Column(modifier = Modifier
        .fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(8.dp)
            .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = { tokenViewModel.getAccessToken() }) {
                Text(text = "Refresh")
            }
            Button(
                onClick = { tokenViewModel.forceRefresh() }) {
                Text(text = "ForceRefresh")
            }
            Button(
                onClick = { tokenViewModel.setNullState() }) {
                Text(text = "Clear")
            }
        }
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
                text = tokenState.accessToken?.toJson()?.let {
                    JSONObject(it).toString(4)
                } ?: "")
        }
        tokenState.exception?.apply {
            Alert(throwable = this)
        }
    }
}

