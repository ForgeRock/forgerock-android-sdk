package org.forgerock.android.auth.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.forgerock.android.auth.Config
import org.forgerock.android.auth.SessionManager

class SSOBroadcastReceiver(private val sessionBuilder: SSOSessionBuilder = SSOSessionBuilder()): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val sessionManager: SessionManager = sessionBuilder.createConfigSessionManager(context)
        Log.d("TESTING------>", "TESTING----->")
        Log.d("TESTING------>", Config.getInstance().toString())
        sessionManager.close()
    }
}

class SSOSessionBuilder {
    fun createConfigSessionManager(context: Context?): SessionManager {
        Config.getInstance().init(context)
        return Config.getInstance().sessionManager
    }
}