/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.journey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.app.PreferenceViewModel
import com.example.app.Topbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyRoute(modifier: Modifier,
                 preferenceViewModel: PreferenceViewModel,
                 openDrawer: () -> Unit,
                 onSubmit: (String) -> Unit) {

    //Stateful Composable - state maintain when configuration change
    var journeyName by rememberSaveable {
        mutableStateOf(preferenceViewModel.getLastJourney())
    }

    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Topbar(heading = "Launch Journey", openDrawer = openDrawer)
            Row(modifier = Modifier.padding(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = journeyName,
                    onValueChange = { value -> journeyName = value },
                    label = { Text("Name") },
                )
                IconButton(onClick = {
                    preferenceViewModel.saveJourney(journeyName)
                    onSubmit(journeyName)
                }) {
                    Icon(Icons.Filled.ArrowCircleRight, contentDescription = null)
                }
            }
        }
    }
}