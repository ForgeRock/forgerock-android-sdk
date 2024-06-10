/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.centralize

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import org.forgerock.android.auth.Result
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.EndSessionResponse
import org.forgerock.android.auth.OAuth2Client
import org.forgerock.android.auth.StringUtils

/**
 * This class is an ActivityResultContract for the OpenID Connect end session process.
 * It creates an intent for the end session request and parses the result of the end session response.
 */
internal class EndSessionContract :
    ActivityResultContract<Pair<String, OAuth2Client>, Result<EndSessionResponse, Throwable>>() {
    /**
     * Creates an intent for the end session request.
     * @param context The context to use for creating the intent.
     * @param input A pair containing the ID token for the session and the configuration for the OpenID Connect client.
     * @return The intent for the end session request.
     */
    override fun createIntent(
        context: Context,
        input: Pair<String, OAuth2Client>,
    ): Intent {
        val configuration =
            AuthorizationServiceConfiguration(
                Uri.parse(input.second.authorizeUrl.toString()),
                Uri.parse(input.second.tokenUrl.toString()),
                null,
                Uri.parse(input.second.endSessionUrl.toString()),
            )

        val builder =
            EndSessionRequest.Builder(configuration)
                .setPostLogoutRedirectUri(Uri.parse(input.second.signOutRedirectUri))

        if (StringUtils.isNotEmpty(input.first)) {
            builder.setIdTokenHint(input.first)
        }

        val authService = AuthorizationService(context)
        return authService.getEndSessionRequestIntent(builder.build())
    }

    /**
     * Parses the result of the end session response.
     * @param resultCode The result code from the activity result.
     * @param intent The intent containing the end session response.
     * @return A boolean indicating whether the session was ended successfully.
     */
    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Result<EndSessionResponse, Throwable> {
        intent?.let { i ->
            val resp = EndSessionResponse.fromIntent(i)
            resp?.let { Result.Success(it) }
            val ex = AuthorizationException.fromIntent(i)
            ex?.let { Result.Failure(it) }
        }
        return Result.Failure(IllegalStateException("End session response is null"))
    }
}
