/*
 * Copyright (c) 2023 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.example.app.selfservice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.app.Alert
import kotlinx.serialization.json.Json
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.selfservice.BindingDevice
import org.forgerock.android.auth.selfservice.Device
import org.forgerock.android.auth.selfservice.ProfileDevice
import org.forgerock.android.auth.selfservice.WebAuthnDevice

@Composable
fun SelfServiceRoute(selfServiceViewModel: SelfServiceViewModel) {

    val state by selfServiceViewModel.state.collectAsState()

    var showDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    var showUpdateConfirmation by remember {
        mutableStateOf(false)
    }

    var showDeviceDetail by remember {
        mutableStateOf(false)
    }

    var selected: Device? by remember {
        mutableStateOf(null)
    }

    var deviceName: String by rememberSaveable {
        mutableStateOf("")
    }

    state.throwable?.apply {
        Alert(state.throwable) {
            selfServiceViewModel.clear()
        }
    }

    val json = Json {
        prettyPrint = true
    }

    if (showDeviceDetail) {
        selected?.let {
            Alert(json.encodeToString(Device.serializer(), it)) {
                showDeviceDetail = false
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            confirmButton = {
                TextButton(onClick = {
                    selected?.let {
                        selfServiceViewModel.delete(it)
                    }
                    showDeleteConfirmation = false
                })
                { Text(text = "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false })
                { Text(text = "Cancel") }
            },
            text = {
                selected?.let {
                    Text(text = "Are you sure you want to delete ${it.deviceName}?")
                }
            }
        )
    }

    if (showUpdateConfirmation) {
        Dialog(
            onDismissRequest = { showUpdateConfirmation = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(375.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Update Device Name",
                        modifier = Modifier.padding(16.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            value = deviceName,
                            onValueChange = { value -> deviceName = value },
                            label = { Text("Device Name") }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextButton(
                            onClick = { showUpdateConfirmation = false },
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                showUpdateConfirmation = false
                                selected?.let {
                                    if (it is WebAuthnDevice) it.deviceName = deviceName
                                    if (it is BindingDevice) it.deviceName = deviceName
                                    if (it is ProfileDevice) it.deviceName = deviceName
                                    selfServiceViewModel.update(it)
                                    deviceName = ""
                                }
                            },
                            modifier = Modifier.padding(8.dp),
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Row(modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()) {
            DeviceDropdownMenu() {
                selfServiceViewModel.fetch(it)
            }
        }
        SelfServiceList(state.devices,
            onShow = {
                showDeviceDetail = true
                selected = it
            },
            onUpdate = {
                showUpdateConfirmation = true
                selected = it
            },
            onDelete = {
                showDeleteConfirmation = true
                selected = it
            }
        )
    }
}

@Composable
fun SelfServiceList(devices: List<Device>,
                    onShow: (Device) -> Unit,
                    onUpdate: (Device) -> Unit,
                    onDelete: (Device) -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        LazyColumn(modifier = Modifier) {
            devices.forEach {
                item {
                    Device(it, onShow, onUpdate, onDelete)
                }
            }
        }
    }
}

@Composable
private fun Device(device: Device,
                   onShow: (Device) -> Unit,
                   onUpdate: (Device) -> Unit,
                   onDelete: (Device) -> Unit) {
    Column {
        Row(modifier = Modifier.padding(4.dp)) {
            Text(text = device.deviceName,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onShow(device)
                    })
        }
        Row(modifier = Modifier.padding(4.dp)) {
            Text(text = device.id,
                modifier = Modifier
                    .fillMaxWidth())
        }
        Row(modifier = Modifier.padding(4.dp)) {
            Spacer(modifier = Modifier.weight(1f))
            if (device is WebAuthnDevice || device is BindingDevice || device is ProfileDevice) {
                Column(modifier = Modifier.padding(2.dp)) {
                    Button(
                        onClick = {
                            onUpdate(device)
                        }) {
                        Icon(Icons.Filled.Update, null)
                    }
                }
            }
            Column(modifier = Modifier.padding(2.dp)) {
                Button(
                    onClick = {
                        onDelete(device)
                    }) {
                    Icon(Icons.Filled.Delete, null)
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDropdownMenu(onSelected: (String) -> Unit) {
    val options = listOf("Oath", "Push", "WebAuthn", "Binding", "Profile")
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(options[0]) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            // The `menuAnchor` modifier must be passed to the text field to handle
            // expanding/collapsing the menu on click. A read-only text field has
            // the anchor type `PrimaryNotEditable`.
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            value = text,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Label") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        text = option
                        expanded = false
                        onSelected(option)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
