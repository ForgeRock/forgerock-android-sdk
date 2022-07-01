/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import android.content.Context
import android.content.SharedPreferences

interface ConfigInterface {
    fun loadFromPreference(context: Context): FROptions
}

class ConfigHelper: ConfigInterface {

    companion object {

        const val ORG_FORGEROCK_V_1_HOSTS = "org.forgerock.v1.HOSTS"

        @JvmStatic
        fun persist(context: Context, frOptions: FROptions) {
            val sharedPreferences =
                context.getSharedPreferences(ORG_FORGEROCK_V_1_HOSTS, Context.MODE_PRIVATE)
            sharedPreferences.edit().putString("url", frOptions.server.url)
                .putString("realm", frOptions.server.realm)
                .putString("cookieName", frOptions.server.cookieName)
                .putString("client_id", frOptions.oauth.oauthClientId)
                .putString("revoke_endpoint", frOptions.urlPath.revokeEndpoint)
                .putString("end_session_endpoint", frOptions.urlPath.endSessionEndpoint).apply()
        }

        @JvmStatic
        fun isConfigChanged(context: Context, frOption: FROptions): Boolean {
            val sharedPreferences =
                context.getSharedPreferences(ORG_FORGEROCK_V_1_HOSTS, Context.MODE_PRIVATE)
            sharedPreferences.getString("url", null)?.apply {
                if(frOption.server.url != this) {
                    return true
                }
            }
            sharedPreferences.getString("realm", null)?.apply {
                if(frOption.server.realm != this) {
                    return true
                }
            }
            sharedPreferences.getString("client_id", null)?.apply {
                if(frOption.oauth.oauthClientId != this) {
                    return true
                }
            }
            sharedPreferences.getString("cookieName", null)?.apply {
                if(frOption.server.cookieName != this) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun load(context: Context, frOption: FROptions?): FROptions {
            return frOption ?: FROptionsBuilder.build {
                server {
                    url = context.getString(R.string.forgerock_url)
                    realm = context.getString(R.string.forgerock_realm)
                    timeout = context.resources.getInteger(R.integer.forgerock_timeout)
                    cookieName = context.getString(R.string.forgerock_cookie_name)
                    oauthUrl = context.getString(R.string.forgerock_oauth_url)
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
                    cookieCacheSeconds =
                        context.resources.getInteger(R.integer.forgerock_cookie_cache)
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
                    logoutEndpoint = context.getString(R.string.forgerock_logout_endpoint)
                    endSessionEndpoint = context.getString(
                        R.string.forgerock_endsession_endpoint
                    )
                }
            }
        }
    }

    override fun loadFromPreference(context: Context): FROptions {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(ORG_FORGEROCK_V_1_HOSTS, Context.MODE_PRIVATE)
            return FROptionsBuilder.build {
                server {
                    url = sharedPreferences.getString("url", null) ?: ""
                    realm = sharedPreferences.getString("realm", null) ?: ""
                    cookieName = sharedPreferences.getString("cookieName", null)
                }
                oauth {
                    oauthClientId = sharedPreferences.getString("client_id", null) ?: ""
                }
                urlPath {
                    endSessionEndpoint = sharedPreferences.getString("end_session_endpoint", null)
                    revokeEndpoint = sharedPreferences.getString("revoke_endpoint", null)
                }
            }
    }
}
