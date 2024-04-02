/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.centralize

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationResponse
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.FRUser
import org.forgerock.android.auth.Listener

/**
 * Singleton class to launch the Browser
 * Centralize login browser launcher.
 */
internal object BrowserLauncher {

    private var launcher: Launcher? = null

    /**
     * Initialize the Launcher
     */
    @Synchronized
    internal fun init(launcher: Launcher) {
        this.launcher = launcher
    }

    /**
     * reset the Launcher state
     */
    @Synchronized
    internal fun reset() {
        launcher?.authorize?.unregister()
        launcher = null
    }

    /**
     * Authorize the user using the Browser
     */
    suspend fun authorize(browser: FRUser.Browser): AuthorizationResponse {
        return launcher?.authorize(browser)
            ?: throw IllegalStateException("Launcher is not initialized")
    }

    /**
     * Authorize the user using the Browser
      */
    fun authorize(browser: FRUser.Browser, listener: FRListener<AuthorizationResponse>): Boolean {
        return launcher?.let {
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                try {
                    val result = authorize(browser)
                    Listener.onSuccess(listener, result)
                } catch (e: Exception) {
                    Listener.onException(listener, e)
                }
            }
            return true
        } ?: false
    }
}