/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.app.R
import org.forgerock.android.auth.callback.SelectIdPCallback

private const val LOCAL_AUTHENTICATION = "localAuthentication"

@Composable
fun SelectIdPCallback(callback: SelectIdPCallback, onSelected: () -> Unit) {

    Row(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start) {

        callback.providers.forEach {
            if (it.provider.equals(LOCAL_AUTHENTICATION)) {
                callback.setValue(LOCAL_AUTHENTICATION)
            }
            if (it.provider.lowercase().contains("facebook")) {
                IconButton(
                    onClick = {
                        callback.setValue(it.provider)
                        onSelected()
                    }) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(R.drawable.com_facebook_favicon_blue),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            }
            if (it.provider.lowercase().contains("apple")) {
                IconButton(
                    onClick = {
                        callback.setValue(it.provider)
                        onSelected()
                    }) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(R.drawable.apple_black),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            }
            if (it.provider.lowercase().contains("google")) {
                IconButton(
                    onClick = {
                        callback.setValue(it.provider)
                        onSelected()
                    }) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(R.drawable.googleg_standard_color_18),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            }
        }
    }

}