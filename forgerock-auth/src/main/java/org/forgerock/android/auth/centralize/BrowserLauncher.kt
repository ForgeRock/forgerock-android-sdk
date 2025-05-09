/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.centralize

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.EndSessionResponse
import org.forgerock.android.auth.FRUser.Browser

/**
 * This object is responsible for launching the browser for OpenID Connect operations.
 */
object BrowserLauncher {

    private val isInitialized = MutableStateFlow(false)

    private var launcher: Launcher? = null

    /**
     * Initializes the launcher.
     * @param launcher The launcher to initialize.
     */
    @Synchronized
    internal fun init(launcher: Launcher) {
        this.launcher = launcher
        isInitialized.value = true
    }

    /**
     * Resets the launcher.
     */
    @Synchronized
    internal fun reset() {
        launcher?.authorize?.first?.unregister()
        launcher?.endSession?.first?.unregister()
        isInitialized.value = false
        launcher = null
    }

    /**
     * Starts the authorization process.
     *
     * @param browser The configuration for the OpenID Connect client.
     * @param pending A boolean indicating whether the authorization process is pending.
     * @return The authorization code.
     * @throws IllegalStateException If the BrowserLauncherActivity is not initialized.
     */
    internal suspend fun authorize(
        browser: Browser,
        pending: Boolean = false,
    ): AuthorizationResponse {
        // Wait until the launcher is initialized
        // The launcher is initialized in the AppAuthFragment2 onCreate method
        return isInitialized.first { it }.let {
            launcher?.authorize(browser, pending)
                ?: throw IllegalStateException("BrowserLauncherActivity not initialized")
        }
    }

    /**
     * Ends the session.
     *
     * @param oauth2Client The configuration for the OpenID Connect client.
     * @param idToken The ID token for the session.
     * @param pending A boolean indicating whether the session end process is pending.
     * @return A boolean indicating whether the session was ended successfully.
     * @throws IllegalStateException If the BrowserLauncherActivity is not initialized.
     */
    internal suspend fun endSession(
        input: EndSessionInput,
        pending: Boolean = false,
    ): EndSessionResponse {
        // Wait until the launcher is initialized
        // The launcher is initialized in the AppAuthFragment2 onCreate method
        return isInitialized.first { it }.let {
            launcher?.endSession(input,
                pending)
                ?: throw IllegalStateException("BrowserLauncherActivity not initialized")
        }
    }
}
