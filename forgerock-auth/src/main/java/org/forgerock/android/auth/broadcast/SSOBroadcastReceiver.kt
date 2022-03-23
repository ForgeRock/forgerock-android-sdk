/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.forgerock.android.auth.Config

/**
 * Broadcast receiver to receive the logout SSO message
 */

class SSOBroadcastReceiver(private val instance: Config = Config.getInstance()): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            instance.init(it)
            instance.sessionManager.close()
        }
    }
}