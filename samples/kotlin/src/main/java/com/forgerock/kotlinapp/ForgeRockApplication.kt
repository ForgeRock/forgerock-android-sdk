/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.forgerock.kotlinapp

import android.app.Application
import android.util.Log
import org.forgerock.android.auth.FRAuth
import org.forgerock.android.auth.FROptionsBuilder
import org.forgerock.android.auth.Logger

class ForgeRockApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        val options = FROptionsBuilder.build {
            server {
                url = "https://openam-forgerrock-sdks.forgeblocks.com/am"
                realm = "alpha"
                cookieName = "96ceb358b4f5946"
                timeout = 300
            }
            oauth {
                oauthClientId = "AndroidTest"
                oauthRedirectUri = "org.forgerock.demo:/oauth2redirect"
                oauthScope = "openid profile email address phone READ_TRANSACTION WRITE_TRANSACTION"
                oauthThresholdSeconds = 60

            }
            service {
                authServiceName = "JeyDeviceProfileCallbackTest1"
            }
        }
        FRAuth.start(this, options)
        Logger.set(Logger.Level.DEBUG)
    }
}
