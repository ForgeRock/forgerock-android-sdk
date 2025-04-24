/*
 * Copyright (c) 2020 - 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.util.Consumer
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import java.net.MalformedURLException

/**
 * AppAuth Integration, https://github.com/openid/AppAuth-Android
 * this class provides AppAuth integration and customization
 */
class AppAuthConfigurer(private val parent: FRUser.Browser) {

    var authorizationRequestBuilder: Consumer<AuthorizationRequest.Builder> =
        Consumer { builder: AuthorizationRequest.Builder? -> }
        private set
    var appAuthConfigurationBuilder: Consumer<AppAuthConfiguration.Builder> =
        Consumer { builder: AppAuthConfiguration.Builder? -> }
        private set
    var customTabsIntentBuilder: Consumer<CustomTabsIntent.Builder> =
        Consumer { builder: CustomTabsIntent.Builder? -> }
        private set
    var authorizationServiceConfigurationSupplier: Supplier<AuthorizationServiceConfiguration> =
        Supplier {
            val oAuth2Client = Config.getInstance().oAuth2Client
            try {
                return@Supplier AuthorizationServiceConfiguration(
                    Uri.parse(oAuth2Client.authorizeUrl.toString()),
                    Uri.parse(oAuth2Client.tokenUrl.toString()),
                    null,
                    Uri.parse(oAuth2Client.endSessionUrl.toString()))
            } catch (e: MalformedURLException) {
                throw RuntimeException(e)
            }
        }
        private set

    /**
     * Override the default OAuth2 endpoint defined under the String.xml forgerock_url.
     *
     * @param authorizationServiceConfiguration [Supplier] that provide the [AuthorizationServiceConfiguration],
     * oauth2 endpoint.
     * @return This AppAuthConfigurer
     */
    fun authorizationServiceConfiguration(authorizationServiceConfiguration: Supplier<AuthorizationServiceConfiguration>): AppAuthConfigurer {
        this.authorizationServiceConfigurationSupplier = authorizationServiceConfiguration
        return this
    }

    /**
     * Override the [AuthorizationRequest] that was prepared by the SDK.
     * The client_id, response type (code), redirect uri, scope are populated by the configuration defined under
     * string.xml forgerock_oauth_client_id, forgerock_oauth_redirect_uri, forgerock_oauth_scope. Developer can provide more
     * customization on the [AuthorizationRequest] object, for example [AuthorizationRequest.Builder.setPrompt]
     *
     * @param authorizationRequest [java.util.function.Consumer] that override the [AuthorizationRequest],
     * some attributes are pre-populated by the provided
     * [AuthorizationRequest]
     * @return This AppAuthConfigurer
     */
    fun authorizationRequest(authorizationRequest: Consumer<AuthorizationRequest.Builder>): AppAuthConfigurer {
        this.authorizationRequestBuilder = authorizationRequest
        return this
    }

    /**
     * Override the [AppAuthConfiguration] that was prepared by the SDK.
     *
     * @param appAuthConfiguration [java.util.function.Consumer] that override the [AppAuthConfiguration]
     */
    fun appAuthConfiguration(appAuthConfiguration: Consumer<AppAuthConfiguration.Builder>): AppAuthConfigurer {
        this.appAuthConfigurationBuilder = appAuthConfiguration
        return this
    }

    /**
     * Override the [CustomTabsIntent] that was prepared by the SDK.
     *
     * @param customTabsIntent [java.util.function.Consumer] that override the [CustomTabsIntent],
     * possibleUris ([CustomTabManager.createTabBuilder]) is
     * pre-populated by the provided [CustomTabsIntent]
     */
    fun customTabsIntent(customTabsIntent: Consumer<CustomTabsIntent.Builder>): AppAuthConfigurer {
        this.customTabsIntentBuilder = customTabsIntent
        return this
    }

    /**
     * Finish up the AppAuth customization.
     */
    fun done(): FRUser.Browser {
        return parent
    }
}
