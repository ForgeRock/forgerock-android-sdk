/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
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
import org.forgerock.android.auth.FRAuth
import org.forgerock.android.auth.FROptions
import org.forgerock.android.auth.FROptionsBuilder

class EnvViewModel : ViewModel() {

    val servers = mutableListOf<FROptions>()

    val localhost = FROptionsBuilder.build {
        server {
            url = "http://192.168.86.249:8080/openam"
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
    }

    val dbind = FROptionsBuilder.build {
        server {
            url = "https://openam-sdks-dbind.forgeblocks.com/am"
            realm = "alpha"
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
            authServiceName = "login-jey"
        }
        sslPinning {
            pins = listOf("WfA2wp20hOS+8OvRhKg1Rka+LLyuMFTbMB5DZ/DE+xo=")
        }
    }

    val sdk = FROptionsBuilder.build {
        server {
            url = "https://openam-forgerrock-sdks.forgeblocks.com/am"
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
            authServiceName = "WebAuthn-Android-Jey"
        }
    }

    val test = FROptionsBuilder.build {
        server {
            url = "https://openam-forgerrock-sdksteanant.forgeblocks.com/am"
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
            authServiceName = "WebAuthn-Android-Jey"
        }
    }

    //

    var current by mutableStateOf(sdk)
        private set

    init {
        servers.add(localhost)
        servers.add(dbind)
        servers.add(sdk)
        servers.add(test)
    }

    fun select(context: Context, options: FROptions) {
        FRAuth.start(context, options)
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