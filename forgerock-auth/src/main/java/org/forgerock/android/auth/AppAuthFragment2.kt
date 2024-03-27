/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Intent
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.MutableStateFlow
import net.openid.appauth.AuthorizationResponse
import org.forgerock.android.auth.centralize.BrowserLauncher
import org.forgerock.android.auth.centralize.Launcher

private const val PENDING = "pending"

/**
 * Headless Fragment to receive callback result from AppAuth library
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class AppAuthFragment2 : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state: MutableStateFlow<Intent?> = MutableStateFlow(null)
        val delegate =
            registerForActivityResult(AuthorizeContract()) {
                state.value = it
            }

        val pending = savedInstanceState?.getBoolean(PENDING, false) ?: false

        BrowserLauncher.init(Launcher(delegate, state, pending))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(PENDING, true)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        BrowserLauncher.reset()
    }

    companion object {
        val TAG: String = AppAuthFragment2::class.java.name

        /**
         * Initialize the Fragment to receive AppAuth callback event.
         */
        @Synchronized
        @JvmStatic
        fun launch(activity: FragmentActivity, browser: FRUser.Browser) {
            val fragmentManager: FragmentManager = activity.supportFragmentManager
            var current = fragmentManager.findFragmentByTag(TAG) as? AppAuthFragment2
            if (current == null) {
                current = AppAuthFragment2()
                fragmentManager.beginTransaction().add(current, TAG).commitNow()
            }

            BrowserLauncher.authorize(browser, object : FRListener<AuthorizationResponse> {
                override fun onSuccess(result: AuthorizationResponse) {
                    reset(activity, current)
                    browser.listener.onSuccess(result)
                }

                override fun onException(e: Exception) {
                    reset(activity, current)
                    browser.listener.onException(e)
                }

                /**
                 * Once receive the result, reset state.
                 */
                private fun reset(activity: FragmentActivity, fragment: Fragment?) {
                    activity.runOnUiThread {
                        BrowserLauncher.reset()
                    }
                    fragment?.let {
                        activity.runOnUiThread {
                            fragmentManager.beginTransaction().remove(it).commitNow()
                        }
                    }
                }
            })


        }
    }
}
