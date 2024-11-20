/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.callback

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.play.core.integrity.IntegrityServiceException
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.callback.AppIntegrityCallback

@Composable
fun AppIntegrityCallback(callback: AppIntegrityCallback, onCompleted: () -> Unit) {

    val currentOnCompleted by rememberUpdatedState(onCompleted)
    val context = LocalContext.current

    Column(modifier = Modifier
        .padding(8.dp)
        .fillMaxHeight()
        .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Checking App Integrity...")
        Spacer(Modifier.height(8.dp))
        CircularProgressIndicator()

        LaunchedEffect(true) {
            try {
                callback.requestIntegrityToken(context)
                //callback.clearCache()
            } catch (e: IntegrityServiceException) {
                Logger.error("AppIntegrityCallback", e, e.message)
                /*
                when (e.errorCode) {
                    //We can set different error with different condition
                    IntegrityErrorCode.TOO_MANY_REQUESTS,
                    IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE,
                    IntegrityErrorCode.CLIENT_TRANSIENT_ERROR,
                    IntegrityErrorCode.INTERNAL_ERROR -> callback.setClientError("Retry")
                }
                 */
            } catch (e: Exception) {
                Logger.error("AppIntegrityCallback", e, e.message)
            }
            currentOnCompleted()
        }
    }
}

