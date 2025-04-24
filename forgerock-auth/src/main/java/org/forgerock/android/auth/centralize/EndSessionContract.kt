/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.centralize

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import net.openid.appauth.AppAuthConfiguration
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
    ActivityResultContract<EndSessionInput, Result<EndSessionResponse, Throwable>>() {

    private lateinit var authorizationService: AuthorizationService

    /**
     * Creates an intent for the end session request.
     * @param context The context to use for creating the intent.
     * @param input A pair containing the ID token for the session and the configuration for the OpenID Connect client.
     * @return The intent for the end session request.
     */
    override fun createIntent(
        context: Context,
        input: EndSessionInput,
    ): Intent {
        val configuration =
            AuthorizationServiceConfiguration(
                Uri.parse(input.oAuth2Client.authorizeUrl.toString()),
                Uri.parse(input.oAuth2Client.tokenUrl.toString()),
                null,
                Uri.parse(input.oAuth2Client.endSessionUrl.toString()),
            )

        val builder =
            EndSessionRequest.Builder(configuration)
                .setPostLogoutRedirectUri(Uri.parse(input.oAuth2Client.signOutRedirectUri))

        if (StringUtils.isNotEmpty(input.idToken)) {
            builder.setIdTokenHint(input.idToken)
        }

        authorizationService =
            AuthorizationService(context, input.appAuthConfiguration)

        return authorizationService.getEndSessionRequestIntent(builder.build())
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
        authorizationService.dispose()
        intent?.let { i ->
            val resp = EndSessionResponse.fromIntent(i)
            resp?.let {
                return Result.Success(it)
            }
            val ex = AuthorizationException.fromIntent(i)
            ex?.let {
                return Result.Failure(it)
            }
        }
        return Result.Failure(IllegalStateException("End session response is null"))
    }
}

internal data class EndSessionInput(
    val idToken: String,
    val oAuth2Client: OAuth2Client,
    val appAuthConfiguration: AppAuthConfiguration =  AppAuthConfiguration.DEFAULT
)
