/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.centralize

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.EndSessionResponse
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.FRUser
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.OAuth2Client
import org.forgerock.android.auth.Result
import org.forgerock.android.auth.exception.BrowserAuthenticationException

private const val PENDING = "pending"

/**
 * Headless Fragment to receive callback result from AppAuth library
 */
internal class AppAuthFragment2 : Fragment() {

    private var pending: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state: MutableStateFlow<Result<AuthorizationResponse, Throwable>?> =
            MutableStateFlow(null)

        val delegate =
            registerForActivityResult(AuthorizeContract()) {
                parentFragmentManager.beginTransaction().remove(this).commit()
                BrowserLauncher.reset()

                when (it) {
                    is Result.Failure -> state.value = Result.Failure(it.value)
                    is Result.Success -> {
                        state.value = Result.Success(it.value)
                    }

                    else -> {
                        state.value =
                            Result.Failure(BrowserAuthenticationException("Unknown Error"))
                    }
                }
            }

        val endSessionState: MutableStateFlow<Result<EndSessionResponse, Throwable>?> =
            MutableStateFlow(null)
        val endSession =
            registerForActivityResult(EndSessionContract()) {
                parentFragmentManager.beginTransaction().remove(this).commit()
                BrowserLauncher.reset()

                when (it) {
                    is Result.Failure -> endSessionState.value = Result.Failure(it.value)
                    is Result.Success -> {
                        endSessionState.value = Result.Success(it.value)
                    }

                    else -> {
                        endSessionState.value =
                            Result.Failure(BrowserAuthenticationException("Unknown Error"))
                    }
                }
            }

        pending = savedInstanceState?.getBoolean(PENDING, false) ?: false

        BrowserLauncher.init(
            Launcher(
                Pair(delegate, state),
                Pair(endSession, endSessionState),
            ),
        )

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
        fun authorize(activity: FragmentActivity,
                      browser: FRUser.Browser,
                      listener: FRListener<AuthorizationResponse>) {
            val fragmentManager: FragmentManager = activity.supportFragmentManager
            var current = fragmentManager.findFragmentByTag(TAG) as? AppAuthFragment2
            if (current == null) {
                current = AppAuthFragment2()
                fragmentManager.beginTransaction().add(current, TAG).commitNow()
            }

            CoroutineScope(Dispatchers.Default).launch {
                try {
                    listener.onSuccess(BrowserLauncher.authorize(browser, current.pending))
                } catch (e: Exception) {
                    listener.onException(e)
                }
            }
        }

        @Synchronized
        @JvmStatic
        fun endSession(
            input: EndSessionInput,
            listener: FRListener<EndSessionResponse>) {
            endSession(InitProvider.getCurrentActivityAsFragmentActivity(),
                input,
                listener)
        }

        @Synchronized
        @JvmStatic
        fun endSession(activity: FragmentActivity,
                       input: EndSessionInput,
                       listener: FRListener<EndSessionResponse>) {
            val fragmentManager: FragmentManager = activity.supportFragmentManager
            var current = fragmentManager.findFragmentByTag(TAG) as? AppAuthFragment2
            if (current == null) {
                current = AppAuthFragment2()
                fragmentManager.beginTransaction().add(current, TAG).commitNow()
            }

            CoroutineScope(Dispatchers.Default).launch {
                try {
                    listener.onSuccess(BrowserLauncher.endSession(input,
                        current.pending))
                } catch (e: Exception) {
                    listener.onException(e)
                }
            }
        }
    }
}