package org.forgerock.android.auth.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.forgerock.android.auth.Config

class SSOBroadcastReceiver(private val instance: Config = Config.getInstance()): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        instance.init(context)
        instance.sessionManager.close()
    }
}