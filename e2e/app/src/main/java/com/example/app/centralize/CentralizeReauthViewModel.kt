/*
 * Copyright (c) 2023- 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.centralize

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.forgerock.android.auth.Action
import org.forgerock.android.auth.FRRequestInterceptor
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.NodeListener
import org.forgerock.android.auth.Request
import org.forgerock.android.auth.RequestInterceptorRegistry

private val TAG = CentralizeReauthViewModel::class.java.simpleName

/**
 * Central login (browser/AppAuth) followed by a re-authentication Journey against AM, with
 * `forceAuth`/`noSession` parameters injected on the authenticate request via
 * [RequestInterceptorRegistry] — the manual harness for SDKS-3046.
 *
 * The SDK was authenticated via Centralized Login, so no SSOToken is stored when the Journey
 * starts; before SDKS-3046 the received session token triggered `revokeAndEndSession` and wiped
 * the still-valid OAuth2.0 tokens. After the fix the new token is persisted without revoking.
 *
 * Run the Central Login first (this screen only shows after a successful central login), then
 * press one of the use-case buttons. Each run replaces the registered request interceptor, and
 * the status readout below reports the session/token state — use Logout to return to the
 * centralized-login precondition before re-running a use case.
 */
class CentralizeReauthViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext

    var state = MutableStateFlow(CentralizeReauthState())
        private set

    private val nodeListener = object : NodeListener<FRSession> {
        override fun onSuccess(result: FRSession) {
            state.update { it.copy(exception = null, session = result) }
            reportState()
        }

        override fun onException(e: Exception) {
            state.update { it.copy(exception = e) }
            reportState()
        }

        override fun onCallbackReceived(node: Node) {
            state.update { it.copy(node = node, exception = null) }
            reportState()
        }
    }

    /**
     * Use Case 1: Central login, then `FRSession.authenticate` with `forceAuth=true`.
     */
    fun useCase1(context: Context) {
        runJourney(context, noSession = false)
    }

    /**
     * Use Case 2: Central login, then `FRSession.authenticate` with `forceAuth=true&noSession=true`.
     */
    fun useCase2(context: Context) {
        runJourney(context, noSession = true)
    }

    private fun runJourney(context: Context, noSession: Boolean) {
        state.update {
            it.copy(node = null, exception = null, session = null)
        }
        //Central login case: no SSO token is stored before the Journey. Replacing the registry is
        //intentional — this harness owns the interceptor for the duration of the run.
        RequestInterceptorRegistry.getInstance().register(
            FRRequestInterceptor { request: Request, tag: Action ->
                if (tag.type == Action.START_AUTHENTICATE) {
                    var url = Uri.parse(request.url().toString())
                        .buildUpon()
                        .appendQueryParameter("forceAuth", "true")
                    if (noSession) {
                        url = url.appendQueryParameter("noSession", "true")
                    }
                    return@FRRequestInterceptor request.newBuilder()
                        .url(url.build().toString())
                        .build()
                }
                request
            } as FRRequestInterceptor<Action>)

        FRSession.authenticate(appContext, "sdkUsernamePasswordJourney", nodeListener)
    }

    fun next(context: Context, node: Node) {
        viewModelScope.launch {
            node.next(context, nodeListener)
        }
    }

    /**
     * Snapshot the session/token state after each Journey event, using non-destructive local
     * reads only — deliberately NOT SessionManager.getAccessToken(), whose interceptor chain
     * can itself trigger a lazy revoke when binding validation fails.
     */
    private fun reportState() {
        viewModelScope.launch {
            try {
                val sessionManager = org.forgerock.android.auth.Config.getInstance().getSessionManager()
                state.update {
                    it.copy(
                        sessionTokenPresent = sessionManager.getSingleSignOnManager().hasToken(),
                        oauth2TokenPresent = sessionManager.getTokenManager().hasToken())
                }
            } catch (e: Exception) {
                Logger.error(TAG, e.message)
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CentralizeReauthViewModel(context.applicationContext) as T
                }
            }
    }
}

data class CentralizeReauthState(
    val node: Node? = null,
    val exception: Exception? = null,
    val session: FRSession? = null,
    val sessionTokenPresent: Boolean = false,
    val oauth2TokenPresent: Boolean = false)
