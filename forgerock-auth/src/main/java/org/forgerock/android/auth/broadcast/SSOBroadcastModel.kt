/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.broadcast

import android.content.Context
import android.content.Intent
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.R


/**
 * SSOBroadcastModel to broadcast the SSO SignOut message to single sign on apps
 */

class SSOBroadcastModel(private val context: Context? = InitProvider.getCurrentActivity(),
                        private val broadcastPermission: String? = context?.resources?.getString(R.string.forgerock_sso_permission),
                        private val broadcastIntent: Intent = Intent(context?.resources?.getString(R.string.forgerock_sso_logout))) {

     fun sendBroadcast() {
        if (isBroadcastEnabled() && broadcastPermission != null) {
            context?.sendBroadcast(broadcastIntent, broadcastPermission)
        }
    }

    private fun isBroadcastEnabled(): Boolean {
        val receivers =  context?.packageManager?.queryBroadcastReceivers(broadcastIntent, 0)?.filter { it.activityInfo.permission == broadcastPermission }
        return receivers?.let { it.count() > 0 } ?: false
    }
}
