/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.forgerock.android.auth.BroadcastConst.broadcastPackageKey

/**
 * Broadcast receiver to receive the logout SSO message
 */

class SSOBroadcastReceiver(private var config: Config? = null): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.getStringExtra(broadcastPackageKey) != context?.packageName
            && context != null
            && intent?.action == context.resources?.getString(R.string.forgerock_sso_logout)) {
            try {
                (config ?: ConfigHelper.getPersistedConfig(context, null))
                    .tokenManager.revoke(null)
            }
            catch (e: Exception) {
                Logger.warn(SSOBroadcastReceiver::class.java.simpleName, e.message)
            }
        }
    }
}