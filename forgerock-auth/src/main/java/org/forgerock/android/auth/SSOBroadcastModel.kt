/*
 *
 *  * Copyright (c) 2022 ForgeRock. All rights reserved.
 *  *
 *  * This software may be modified and distributed under the terms
 *  * of the MIT license. See the LICENSE file for details.
 *
 *
 */

package org.forgerock.android.auth

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_RECEIVER_FOREGROUND


/**
 * SSOBroadcastModel to broadcast the SSO SignOut message to single sign on apps
 */

internal class SSOBroadcastModel(private val context: Context? = InitProvider.getCurrentActivity(),
                        private val broadcastIntent: Intent = Intent(context?.resources?.getString(R.string.forgerock_sso_logout))) {

    private val broadcastPermission: String? = context?.resources?.getString(R.string.forgerock_sso_permission)

    fun sendLogoutBroadcast() {
         context?.let {
             if (isBroadcastEnabled() && broadcastPermission != null) {
                 broadcastIntent.flags = FLAG_RECEIVER_FOREGROUND
                 broadcastIntent.putExtra(BroadcastConst.broadcastPackageKey, it.packageName)
                 it.sendBroadcast(broadcastIntent, broadcastPermission)
             }
         }
    }

    private fun isBroadcastEnabled(): Boolean {
        val receivers =  context?.packageManager?.queryBroadcastReceivers(broadcastIntent, 0)?.filter { it.activityInfo.permission == broadcastPermission }
        return receivers?.let { it.count() > 0 } ?: false
    }
}

object BroadcastConst {
    const val broadcastPackageKey = "BROADCAST_PACKAGE_KEY"
}