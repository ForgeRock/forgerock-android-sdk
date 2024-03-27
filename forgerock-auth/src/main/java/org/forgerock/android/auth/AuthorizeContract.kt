/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.browser.customtabs.CustomTabsIntent
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import org.forgerock.android.auth.FRUser.Browser

/**
 * This class is an implementation of the ActivityResultContract.
 * It is used to handle the OAuth2 authorization process.
 */
internal class AuthorizeContract : ActivityResultContract<Browser, Intent?>() {
    override fun createIntent(
        context: Context,
        input: Browser,
    ): Intent {
        val configurer: AppAuthConfigurer = input.appAuthConfigurer
        val oAuth2Client = Config.getInstance().oAuth2Client

        val configuration = configurer.authorizationServiceConfigurationSupplier.get()
        val authRequestBuilder =
            AuthorizationRequest.Builder(
                configuration,
                oAuth2Client.clientId,
                oAuth2Client.responseType,
                Uri.parse(oAuth2Client.redirectUri),
            ).setScope(oAuth2Client.scope)

        //Allow caller to override Authorization Request setting
        configurer.authorizationRequestBuilder.accept(authRequestBuilder)
        val authorizationRequest = authRequestBuilder.build()

        //Allow caller to override AppAuth default setting
        val appAuthConfigurationBuilder = AppAuthConfiguration.Builder()
        configurer.appAuthConfigurationBuilder.accept(appAuthConfigurationBuilder)
        val authorizationService = AuthorizationService(context, appAuthConfigurationBuilder.build())

        //Allow caller to override custom tabs default setting
        val intentBuilder: CustomTabsIntent.Builder =
            authorizationService.createCustomTabsIntentBuilder(authorizationRequest.toUri())
        configurer.customTabsIntentBuilder.accept(intentBuilder)

        val request = authRequestBuilder.build()
        val service = AuthorizationService(context, AppAuthConfiguration.DEFAULT)
        return service.getAuthorizationRequestIntent(request, intentBuilder.build())
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): Intent? {
        return intent
    }
}