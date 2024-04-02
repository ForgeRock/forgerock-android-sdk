/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.centralize

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import org.forgerock.android.auth.FRUser
import org.forgerock.android.auth.exception.BrowserAuthenticationException

internal class Launcher(val authorize: ActivityResultLauncher<FRUser.Browser>,
                        val state: MutableStateFlow<Intent?>,
                        private val pending: Boolean = false) {
    suspend fun authorize(request: FRUser.Browser): AuthorizationResponse {
        //If waiting for response, we don't launch the browser again
        if (!pending) {
            authorize.launch(request)
        }
        //drop the default value
        state.drop(1).first().let {
            it?.let { i ->
                val error = i.getStringExtra(AuthorizationException.EXTRA_EXCEPTION)
                error?.let { e -> throw BrowserAuthenticationException(e) }
                return AuthorizationResponse.fromIntent(i)
                    ?: throw BrowserAuthenticationException("Failed to retrieve authorization code")
            }
            throw BrowserAuthenticationException("No response data")
        }
    }
}