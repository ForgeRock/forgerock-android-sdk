/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.centralize

import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.EndSessionResponse
import org.forgerock.android.auth.FRUser.Browser
import org.forgerock.android.auth.OAuth2Client
import org.forgerock.android.auth.Result
import org.forgerock.android.auth.exception.BrowserAuthenticationException

/**
 * This class is responsible for launching the browser for OpenID Connect operations.
 */
internal class Launcher(
    val authorize: Pair<ActivityResultLauncher<Browser>, MutableStateFlow<Result<AuthorizationResponse, Throwable>?>>,
    val endSession: Pair<ActivityResultLauncher<Pair<String, OAuth2Client>>, MutableStateFlow<Result<EndSessionResponse, Throwable>?>>,
) {
    /**
     * Starts the authorization process.
     * @param request The configuration for the OpenID Connect client.
     * @param pending A boolean indicating whether the authorization process is pending.
     */
    suspend fun authorize(
        request: Browser,
        pending: Boolean = false,
    ): AuthorizationResponse {
        if (!pending) {
            authorize.first.launch(request)
        }

        // drop the default value
        when (val result = authorize.second.drop(1).filterNotNull().first()) {
            is Result.Failure -> throw result.value
            is Result.Success -> return result.value
            else -> throw BrowserAuthenticationException("Unknown Error")
        }
    }

    /**
     * Ends the session.
     * @param request A pair containing the ID token for the session and the configuration for the OpenID Connect client.
     * @param pending A boolean indicating whether the session end process is pending.
     */
    suspend fun endSession(
        request: Pair<String, OAuth2Client>,
        pending: Boolean = false,
    ): EndSessionResponse {
        if (!pending) {
            endSession.first.launch(request)
        }
        // drop the default value
        when (val result = endSession.second.drop(1).filterNotNull().first()) {
            is Result.Failure -> throw result.value
            is Result.Success -> return result.value
            else -> throw BrowserAuthenticationException("Unknown Error")
        }
    }
}
