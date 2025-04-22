/*
 * Copyright (c) 2023 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package com.example.app.env

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.forgerock.android.auth.FRAuth
import org.forgerock.android.auth.FROptions
import org.forgerock.android.auth.FROptionsBuilder
import org.forgerock.android.auth.Logger

const val USER_PROFILE_JOURNEY = "UserProfile"
private val TAG = EnvViewModel::class.java.simpleName

class EnvViewModel : ViewModel() {

    val servers = mutableListOf<FROptions>()

    // Example values for a PingAM instance
    val PingAM = FROptionsBuilder.build {
        server {
            url = "https://openam.example.com:8443/openam"
            realm = "root"
            cookieName = "iPlanetDirectoryPro"
            timeout = 50
        }
        oauth {
            oauthClientId = "sdkPublicClient"
            oauthRedirectUri = "org.forgerock.demo://oauth2redirect"
            oauthScope = "openid profile email address"
            oauthSignOutRedirectUri = "org.forgerock.demo://oauth2redirect"
        }
        service {
            authServiceName = "sdkUsernamePasswordJourney"
        }
    }

    // Example values for a Ping Advanced Identity Cloud instance
    val PingAdvancedIdentityCloud = FROptionsBuilder.build {
        server {
            url = "https://openam-forgerock-sdks.forgeblocks.com/am"
            realm = "alpha"
            cookieName = "29cd7a346b42b42"
            timeout = 50
        }
        oauth {
            oauthClientId = "sdkPublicClient"
            oauthRedirectUri = "org.forgerock.demo://oauth2redirect"
            oauthScope = "openid profile email address"
            oauthSignOutRedirectUri = "org.forgerock.demo://oauth2redirect"
        }
        service {
            authServiceName = "sdkUsernamePasswordJourney"
        }
    }

    // Example values for a PingOne instance
    val PingOne = FROptionsBuilder.build {
        server {
            url = "https://auth.pingone.com/3072206d-c6ce-ch15-m0nd-f87e972c7cc3/as"
        }
        oauth {
            oauthClientId = "6c7eb89a-66e9-ab12-cd34-eeaf795650b2"
            oauthRedirectUri = "org.forgerock.demo://oauth2redirect"
            oauthSignOutRedirectUri = "org.forgerock.demo://oauth2redirect"
            oauthScope = "openid profile email address revoke"
        }
    }

    // Example values for a PingFederate instance
    val PingFederate = FROptionsBuilder.build {
        server {
            url = "https://pingfed.example.com"
        }
        oauth {
            oauthClientId = "sdkPublicClient"
            oauthRedirectUri = "org.forgerock.demo://oauth2redirect"
            oauthSignOutRedirectUri = "org.forgerock.demo://oauth2redirect"
            oauthScope = "openid profile email address"
        }
    }

    var current by mutableStateOf(PingAdvancedIdentityCloud)
        private set

    init {
        servers.add(PingAM)
        servers.add(PingAdvancedIdentityCloud)
        servers.add(PingOne)
        servers.add(PingFederate)
    }

    fun select(context: Context, options: FROptions) {
        // Discover settings from the specified PingOne server's .well-known endpoint
        // For PingOne and PingFederate instances we use the .well-known endpoint to discover the configuration
        if(options === PingOne || options === PingFederate) {
            viewModelScope.launch {
                try {
                    val option = options.discover(options.server.url + "/.well-known/openid-configuration")
                    FRAuth.start(context, option)

                } catch (error: Exception) {
                    Logger.error(TAG, error, "Discovery failed from PingOne .well-known endpoint: ${options.server.url}.\n" +
                            "Message: ${error.message}")
                }
            }
        }
        // Configure the SDKs for a PingAM/Ping Advanced Identity Cloud/PingOne/PingFed instance
        else {
            FRAuth.start(context, options)
        }
        current = options
    }

    fun select(context: Context, host: String) {
        servers.find {
            it.server.url == host
        }?.let {
            select(context, it)
        } ?: run {
            select(context, PingAdvancedIdentityCloud)
        }
    }

    fun getAll(): List<FROptions> {
        return servers
    }
}