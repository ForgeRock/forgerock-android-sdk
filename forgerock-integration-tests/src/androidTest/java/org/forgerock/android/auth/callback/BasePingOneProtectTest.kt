/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.forgerock.android.auth.FRAuth
import org.forgerock.android.auth.FROptionsBuilder
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.Logger.Companion.set
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.Timeout
import java.util.concurrent.TimeUnit

abstract class BasePingOneProtectTest {
    val context: Context = ApplicationProvider.getApplicationContext()
    private val AM_URL = "https://openam-protect2.forgeblocks.com/am"
    private val REALM = "alpha"
    private val COOKIE_NAME = "c1c805de4c9b333"
    private val OAUTH_CLIENT = "AndroidTest"
    private val OAUTH_REDIRECT_URI = "org.forgerock.demo:/oauth2redirect"
    private val SCOPE = "openid profile email address phone"
    companion object {
        var USERNAME = "sdkuser"
    }

    private val options = FROptionsBuilder.build {
        server {
            url = AM_URL
            realm = REALM
            cookieName = COOKIE_NAME
        }
        oauth {
            oauthClientId = OAUTH_CLIENT
            oauthRedirectUri = OAUTH_REDIRECT_URI
            oauthScope = SCOPE
        }
    }

    @Rule @JvmField
    val timeout = Timeout(20000, TimeUnit.MILLISECONDS)

    @Before
    fun setUpSDK() {
        set(Logger.Level.DEBUG)
        FRAuth.start(context, options)
    }

    @After
    fun logoutSession() {
        FRSession.getCurrentSession()?.let {
            it.logout()
        }
    }
}
