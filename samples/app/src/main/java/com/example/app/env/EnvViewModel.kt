/*
 * Copyright (c) 2023-2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.env

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.forgerock.android.auth.FRAuth
import org.forgerock.android.auth.FROptions
import org.forgerock.android.auth.FROptionsBuilder

class EnvViewModel : ViewModel() {

    val servers = mutableListOf<FROptions>()

    val localhost = FROptionsBuilder.build {
        server {
            url = "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447"
            realm = "alpha"
            cookieName = "c1c805de4c9b333"
            timeout = 50
        }
        oauth {
            oauthClientId = "c12743f9-08e8-4420-a624-71bbb08e9fe1"
            oauthRedirectUri = "org.forgerock.demo://oauth2redirect"
            oauthCacheSeconds = 0
            oauthScope = "openid profile email address phone"
            oauthThresholdSeconds = 0
            oauthSignOutRedirectUri = "org.forgerock.demo://oauth2redirect"
        }
        urlPath {
            authorizeEndpoint = "/as/authorize"
            tokenEndpoint = "/as/token"
            endSessionEndpoint = "/as/signoff"
            revokeEndpoint = "/as/revoke"
            userinfoEndpoint = "/as/userinfo"
        }
        service {
            authServiceName = "protect"
        }
    }

    val dbind = FROptionsBuilder.build {
        server {
            url = "https://openam-updbind.forgeblocks.com/am"
            realm = "bravo"
            cookieName = "ccdd0582e7262db"
            timeout = 50
        }
        oauth {
            oauthClientId = "AndroidTest"
            oauthRedirectUri = "org.forgerock.demo:/oauth2redirect"
            oauthCacheSeconds = 0
            oauthScope = "openid profile email address phone"
            oauthThresholdSeconds = 0
        }
        service {
            authServiceName = "sign-verifier-stoyan"
        }
    }

    val sdk = FROptionsBuilder.build {
        server {
            url = "https://openam-dbind.forgeblocks.com/am"
            realm = "alpha"
            cookieName = "43d72fc37bdde8c"
            timeout = 50
        }
        oauth {
            oauthClientId = "AndroidTest"
            oauthRedirectUri = "org.forgerock.demo:/oauth2redirect"
            oauthCacheSeconds = 0
            oauthScope = "openid profile email address phone"
            oauthThresholdSeconds = 0
        }
        service {
            authServiceName = "WebAuthn"
        }
    }

    val local = FROptionsBuilder.build {
        server {
            url = "https://andy.petrov.ca/openam"
            realm = "root"
            cookieName = "iPlanetDirectoryPro"
            timeout = 50
        }
        oauth {
            oauthClientId = "AndroidTest"
            oauthRedirectUri = "org.forgerock.demo:/oauth2redirect"
            oauthCacheSeconds = 0
            oauthScope = "openid profile email address phone"
            oauthThresholdSeconds = 0
        }
        service {
            authServiceName = "WebAuthn"
        }
    }

    val ops = FROptionsBuilder.build {
        server {
            url = "https://default.forgeops.petrov.ca/am"
            realm = "root"
            cookieName = "iPlanetDirectoryPro"
            timeout = 50
        }
        oauth {
            oauthClientId = "AndroidTest"
            oauthRedirectUri = "org.forgerock.demo:/oauth2redirect"
            oauthCacheSeconds = 60
            oauthScope = "openid profile email address phone"
            oauthThresholdSeconds = 0
        }
        service {
            authServiceName = "WebAuthn"
        }
    }

    val forgeblock = FROptionsBuilder.build {
        server {
            url = "https://openam-sdks.forgeblocks.com/am"
            realm = "alpha"
            cookieName = "iPlanetDirectoryPro"
            timeout = 50
        }
        oauth {
            oauthClientId = "AndroidTest"
            oauthRedirectUri = "org.forgerock.demo:/oauth2redirect"
            oauthCacheSeconds = 0
            oauthScope = "openid profile email address phone"
            oauthThresholdSeconds = 0
        }
        service {
            authServiceName = "WebAuthn"
        }
    }

    val pingOidc = FROptionsBuilder.build {
        server {
            url = "https://auth.pingone.ca/02fb4743-189a-4bc7-9d6c-a919edfe6447/as"
        }
        oauth {
            oauthClientId = "c12743f9-08e8-4420-a624-71bbb08e9fe1"
            oauthRedirectUri = "org.forgerock.demo://oauth2redirect"
            oauthScope = "openid email address phone profile revoke"
        }
    }

    var current by mutableStateOf(dbind)
        private set

    init {
        servers.add(localhost)
        servers.add(dbind)
        servers.add(sdk)
        servers.add(local)
        servers.add(ops)
        servers.add(forgeblock)
        servers.add(pingOidc)
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
            select(context, dbind)
        }
    }

    fun getAll(): List<FROptions> {
        return servers
    }
}