package com.forgerock.androidapp2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.FRUser

interface BroadcastActivityListener {
    fun updateUI()
}

class TokenBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
       if(intent?.getStringExtra("package") == "appB") {
           return
       }
        FRUser.getCurrentUser()?.logout()
       // listener?.updateUI()
    }
}