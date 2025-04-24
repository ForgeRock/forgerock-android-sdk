/*
 * Copyright (c) 2023 - 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app

import androidx.lifecycle.ViewModel
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.browser.BrowserDenyList
import net.openid.appauth.browser.VersionedBrowserMatcher
import org.forgerock.android.auth.FRUser

class LogoutViewModel : ViewModel() {


    /*
    fun logout() {
        val config = AppAuthConfiguration.Builder()
            .setBrowserMatcher(BrowserDenyList(
                VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                VersionedBrowserMatcher.CHROME_BROWSER
            ))
            .build()

        FRUser.getCurrentUser()?.logout(config)
    }
    */

    fun logout() {
        FRUser.getCurrentUser()?.logout()
    }
}