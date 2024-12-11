/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.forgerock.android.auth.callback.TextInputCallback
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputCallback(textInputCallback: TextInputCallback) {

    var text by remember {
        mutableStateOf("")
    }

    Row(modifier = Modifier
        .padding(4.dp)
        .fillMaxWidth()) {
        OutlinedTextField(
            modifier = Modifier,
            value = text,
            onValueChange = { value ->
                text = value
                textInputCallback.setValue(text)
            },
            label = { Text(textInputCallback.prompt) },
        )
    }

}

@Preview
@Composable
fun TextInputCallbackPreview() {
    val json = JSONObject("{\n" +
            "    \"type\": \"TextInputCallback\",\n" +
            "    \"output\": [\n" +
            "        {\n" +
            "            \"name\": \"prompt\",\n" +
            "            \"value\": \"Enter text\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"defaultText\",\n" +
            "            \"value\": \"\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"input\": [\n" +
            "        {\n" +
            "            \"name\": \"IDToken1\",\n" +
            "            \"value\": \"\"\n" +
            "        }\n" +
            "    ]\n" +
            "}")
    TextInputCallback(TextInputCallback(json, 0))
}