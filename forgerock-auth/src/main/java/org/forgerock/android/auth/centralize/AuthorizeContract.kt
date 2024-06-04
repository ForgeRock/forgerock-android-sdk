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
import androidx.browser.customtabs.CustomTabsIntent
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import org.forgerock.android.auth.AppAuthConfigurer
import org.forgerock.android.auth.Config
import org.forgerock.android.auth.FRUser.Browser
import org.forgerock.android.auth.Result
import org.forgerock.android.auth.exception.BrowserAuthenticationException

/**
 * This class is an [ActivityResultContract] for the OpenID Connect authorization process.
 * It creates an intent for the authorization request and parses the result of the authorization response.
 */
internal class AuthorizeContract :
    ActivityResultContract<Browser, Result<AuthorizationResponse, Throwable>>() {
    /**
     * Creates an intent for the authorization request.
     *
     * @param context The context to use for creating the intent.
     * @param input The configuration for the OpenID Connect client.
     * @return The intent for the authorization request.
     */
    override fun createIntent(
        context: Context,
        input: Browser,
    ): Intent {
        val oAuth2Client = Config.getInstance().oAuth2Client
        val configuration =
            AuthorizationServiceConfiguration(
                Uri.parse(oAuth2Client.authorizeUrl.toString()),
                Uri.parse(oAuth2Client.tokenUrl.toString()),
            )
        val builder =
            AuthorizationRequest.Builder(
                configuration,
                oAuth2Client.clientId,
                oAuth2Client.responseType,
                Uri.parse(oAuth2Client.redirectUri),
            ).setScope(oAuth2Client.scope)

        //Allow caller to override Authorization Request setting
        val configurer: AppAuthConfigurer = input.appAuthConfigurer()
        configurer.authorizationRequestBuilder.accept(builder)
        val authorizationRequest = builder.build()

        //Allow caller to override AppAuth default setting
        val appAuthConfigurationBuilder = AppAuthConfiguration.Builder()
        configurer.appAuthConfigurationBuilder.accept(appAuthConfigurationBuilder)
        val authorizationService =
            AuthorizationService(context, appAuthConfigurationBuilder.build())

        //Allow caller to override custom tabs default setting
        val intentBuilder: CustomTabsIntent.Builder =
            authorizationService.createCustomTabsIntentBuilder(authorizationRequest.toUri())
        configurer.customTabsIntentBuilder.accept(intentBuilder)

        val request = builder.build()
        val service = AuthorizationService(context, AppAuthConfiguration.DEFAULT)
        return service.getAuthorizationRequestIntent(request, intentBuilder.build())

    }

    /**
     * Parses the result of the authorization response.
     *
     * @param resultCode The result code of the authorization response.
     * @param intent The intent of the authorization response.
     * @return A Result containing the authorization response or an error.
     */
    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Result<AuthorizationResponse, Throwable> {
        intent?.let { i ->
            val error = AuthorizationException.fromIntent(i)
            error?.let {
                return Result.Failure(
                    BrowserAuthenticationException(
                        "Failed to retrieve authorization code. ${it.message}",
                        it,
                    ),
                )
            }
            val result = AuthorizationResponse.fromIntent(i)
            result?.let {
                return Result.Success(it)
            } ?: return Result.Failure(BrowserAuthenticationException("Failed to retrieve authorization code"))
        }
        return Result.Failure(BrowserAuthenticationException("No response data"))
    }

}