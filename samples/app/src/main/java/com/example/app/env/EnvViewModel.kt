/*
 * Copyright (c) 2023 - 2024 ForgeRock. All rights reserved.
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
import com.example.app.storage.loadCookiesStorage
import com.example.app.storage.loadSSOTokenStorage
import com.example.app.storage.loadTokenStorage
import kotlinx.coroutines.launch
import org.forgerock.android.auth.ContextProvider
import org.forgerock.android.auth.FRAuth
import org.forgerock.android.auth.FROptions
import org.forgerock.android.auth.FROptionsBuilder

class EnvViewModel : ViewModel() {

    val servers = mutableListOf<FROptions>()

    val server = FROptionsBuilder.build {
        server {
            url = "https://<ServerURL>/openam"
            realm = "alpha"
            cookieName = "<cookieName>"
            timeout = 50
        }
        oauth {
            oauthClientId = "<ClientID>"
            oauthRedirectUri = "<RedirectURI>"
            oauthCacheSeconds = 0
            oauthScope = "openid profile email address phone"
            oauthThresholdSeconds = 0
        }
        service {
            authServiceName = "Login"
        }
        store {
            // Override the default storage
            oidcStorage = loadTokenStorage(ContextProvider.context)
            ssoTokenStorage = loadSSOTokenStorage(ContextProvider.context)
            cookiesStorage = loadCookiesStorage(ContextProvider.context)
        }
    }

    var current by mutableStateOf(server)
        private set

    init {
        servers.add(server)
    }

    fun select(context: Context, options: FROptions) {
        if(options.server.url.contains("pingone")) {
            viewModelScope.launch {
                val option =
                    options.discover(options.server.url + "/.well-known/openid-configuration")
                FRAuth.start(context, option)
            }
        }
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
            select(context, server)
        }
    }

    fun getAll(): List<FROptions> {
        return servers
    }
}