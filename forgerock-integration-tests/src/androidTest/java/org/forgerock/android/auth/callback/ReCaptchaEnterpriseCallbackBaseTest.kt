/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.forgerock.android.auth.FRAuth
import org.forgerock.android.auth.FROptionsBuilder
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.Logger.Companion.set
import org.forgerock.android.auth.TestConfig
import org.junit.After
import org.junit.BeforeClass

/**
 * e2e tests for [ReCaptchaEnterpriseCallback]
 */
open class ReCaptchaEnterpriseCallbackBaseTest {
    @After
    fun logoutSession() {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout()
        }
    }

    companion object {
        val context: Context = ApplicationProvider.getApplicationContext()
        val application: Application = ApplicationProvider.getApplicationContext()

        protected val AM_URL: String = TestConfig.serverUrl
        protected val REALM: String = TestConfig.realm
        protected val OAUTH_CLIENT: String = TestConfig.clientId
        protected val OAUTH_REDIRECT_URI: String = TestConfig.redirectUri
        protected val SCOPE: String = TestConfig.scope
        const val TREE: String = "TEST-e2e-recaptcha-enterprise"
        val USERNAME: String = TestConfig.username
        val RECAPTCHA_SITE_KEY: String = TestConfig.recaptchaSiteKey

        @JvmStatic
        @BeforeClass
        fun setUpSDK(): Unit {
            set(Logger.Level.DEBUG)

            val options = FROptionsBuilder.build {
                server {
                    url = AM_URL
                    cookieName = TestConfig.cookieName
                    realm = REALM
                }
                oauth {
                    oauthClientId = OAUTH_CLIENT
                    oauthRedirectUri = OAUTH_REDIRECT_URI
                    oauthCacheSeconds = 0
                    oauthScope = SCOPE
                }
                service {
                    authServiceName = TREE
                }
            }

            FRAuth.start(context, options)
        }
    }
}




