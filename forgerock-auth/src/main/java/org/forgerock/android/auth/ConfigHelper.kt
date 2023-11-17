/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import android.content.Context

/**
 * ConfigHelper Provide the helper methods to persist and load the configurations on start of the SDK
 * This class also verify if the configuration changes on the current and previous launch.
 */
internal class ConfigHelper {

    companion object {

        private const val realm = "realm"
        private const val url = "url"
        private const val cookieName = "cookieName"
        private const val clientId = "client_id"
        private const val revokeEndpoint = "revoke_endpoint"
        private const val endSessionEndpoint = "end_session_endpoint"
        private const val sessionEndpoint = "session_endpoint"
        private const val scope = "scope"
        private const val redirectUri = "redirect_uri"

        //Alias to store Previous Configure Host
        internal const val ORG_FORGEROCK_V_1_HOSTS = "org.forgerock.v1.HOSTS"


        /**
         * Persist the FROption Data in shared preference
         *
         * @param context  The Application Context
         * @param frOptions Argument to persist the data in shared preference using the context
         */
        @JvmStatic
        fun persist(context: Context, frOptions: FROptions) {
            val sharedPreferences =
                context.getSharedPreferences(ORG_FORGEROCK_V_1_HOSTS, Context.MODE_PRIVATE)
            sharedPreferences.edit().putString(url, frOptions.server.url)
                .putString(realm, frOptions.server.realm)
                .putString(cookieName, frOptions.server.cookieName)
                .putString(clientId, frOptions.oauth.oauthClientId)
                .putString(revokeEndpoint, frOptions.urlPath.revokeEndpoint)
                .putString(endSessionEndpoint, frOptions.urlPath.endSessionEndpoint)
                .putString(sessionEndpoint, frOptions.urlPath.sessionEndpoint)
                .putString(scope, frOptions.oauth.oauthScope)
                .putString(redirectUri, frOptions.oauth.oauthRedirectUri)
                .apply()
        }

        /**
         * load the sdk configuration from the persistence
         *
         * @param context  The Application Context
         * @return FROptions from stored persistence
         */
        @JvmStatic
        fun loadFromPreference(context: Context): FROptions {
            val sharedPreferences =
                context.getSharedPreferences(ORG_FORGEROCK_V_1_HOSTS, Context.MODE_PRIVATE)

            return FROptionsBuilder.build {
                server {
                    url = sharedPreferences.getString(ConfigHelper.url, null) ?: context.getString(R.string.forgerock_url)
                    realm = sharedPreferences.getString(ConfigHelper.realm, null) ?: context.getString(R.string.forgerock_realm)
                    cookieName = sharedPreferences.getString(ConfigHelper.cookieName, null) ?: context.getString(R.string.forgerock_cookie_name)
                }
                oauth {
                    oauthClientId = sharedPreferences.getString(ConfigHelper.clientId, null) ?: context.getString(R.string.forgerock_oauth_client_id)
                    oauthScope = sharedPreferences.getString(ConfigHelper.scope, null) ?: context.getString(R.string.forgerock_oauth_scope)
                    oauthRedirectUri = sharedPreferences.getString(ConfigHelper.redirectUri, null) ?: context.getString(R.string.forgerock_oauth_redirect_uri)
                }
                urlPath {
                    endSessionEndpoint =
                        sharedPreferences.getString(ConfigHelper.endSessionEndpoint, null) ?: context.getString(R.string.forgerock_endsession_endpoint)
                    revokeEndpoint = sharedPreferences.getString(ConfigHelper.revokeEndpoint, null) ?: context.getString(R.string.forgerock_revoke_endpoint)
                    sessionEndpoint = sharedPreferences.getString(ConfigHelper.sessionEndpoint, null) ?: context.getString(R.string.forgerock_session_endpoint)
                }
            }
        }

        /**
         * validate the configuration changes on start of the SDK
         *
         * @param context  The Application Context
         * @param frOptions takes current configuration and compare with persisted configuration
         * @return true when the config changed and false if its not changed
         */
        @JvmStatic
        fun isConfigDifferentFromPersistedValue(context: Context, frOptions: FROptions): Boolean {
            val sharedPreferences =
                context.getSharedPreferences(ORG_FORGEROCK_V_1_HOSTS, Context.MODE_PRIVATE)
            sharedPreferences.getString(url, null)?.apply {
                if(frOptions.server.url != this) {
                    return true
                }
            }
            sharedPreferences.getString(realm, null)?.apply {
                if(frOptions.server.realm != this) {
                    return true
                }
            }
            sharedPreferences.getString(clientId, null)?.apply {
                if(frOptions.oauth.oauthClientId != this) {
                    return true
                }
            }
            sharedPreferences.getString(cookieName, null)?.apply {
                if(frOptions.server.cookieName != this) {
                    return true
                }
            }
            sharedPreferences.getString(scope, null)?.apply {
                if(frOptions.oauth.oauthScope != this) {
                    return true
                }
            }
            sharedPreferences.getString(redirectUri, null)?.apply {
                if(frOptions.oauth.oauthRedirectUri != this) {
                    return true
                }
            }
            return false
        }

        /**
         * load the sdk configuration for the caller
         *
         * @param context  The Application Context
         * @param frOptions takes current configuration and compare with persisted configuration
         * @return FROptions from strings.xml or passed value
         */
        @JvmStatic
        fun load(context: Context, frOptions: FROptions?): FROptions {
            return frOptions ?: FROptionsBuilder.build {
                server {
                    url = context.getString(R.string.forgerock_url)
                    realm = context.getString(R.string.forgerock_realm)
                    timeout = context.resources.getInteger(R.integer.forgerock_timeout)
                    cookieName = context.getString(R.string.forgerock_cookie_name)
                    cookieCacheSeconds =
                        context.resources.getInteger(R.integer.forgerock_cookie_cache)
                            .toLong()
                }
                oauth {
                    oauthClientId = context.getString(R.string.forgerock_oauth_client_id)
                    oauthRedirectUri = context.getString(R.string.forgerock_oauth_redirect_uri)
                    oauthScope = context.getString(R.string.forgerock_oauth_scope)
                    oauthThresholdSeconds =
                        context.resources.getInteger(R.integer.forgerock_oauth_threshold).toLong()
                    oauthCacheSeconds =
                        context.resources.getInteger(R.integer.forgerock_oauth_cache)
                            .toLong()
               }
                sslPinning {
                    pins = context.resources
                        .getStringArray(R.array.forgerock_ssl_pinning_public_key_hashes).toList()
                    buildSteps = emptyList()
                }
                service {
                    authServiceName = context.getString(R.string.forgerock_auth_service)
                    registrationServiceName =
                        context.getString(R.string.forgerock_registration_service)
                }
                urlPath {
                    authenticateEndpoint =
                        context.getString(R.string.forgerock_authenticate_endpoint)
                    authorizeEndpoint = context.getString(R.string.forgerock_authorize_endpoint)
                    tokenEndpoint = context.getString(R.string.forgerock_token_endpoint)
                    revokeEndpoint = context.getString(R.string.forgerock_revoke_endpoint)
                    userinfoEndpoint = context.getString(R.string.forgerock_userinfo_endpoint)
                    sessionEndpoint = context.getString(R.string.forgerock_session_endpoint)
                    endSessionEndpoint = context.getString(
                        R.string.forgerock_endsession_endpoint
                    )
                }
            }
        }

        /**
         * get the persisted config
         *
         * @param context  The Application Context
         * @param options  The FrOptions to be passed
         */
        @JvmOverloads
        @JvmStatic
        fun getPersistedConfig(context: Context,
                               options: FROptions? = null): Config {

            val config = Config()
            config.init(context, options ?: loadFromPreference(context))
            return config
        }
    }
}