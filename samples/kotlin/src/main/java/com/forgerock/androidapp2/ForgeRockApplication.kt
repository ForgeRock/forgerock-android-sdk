package com.forgerock.androidapp2

import android.app.Application
import org.forgerock.android.auth.FRAuth
import org.forgerock.android.auth.Logger

class ForgeRockApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.set(Logger.Level.DEBUG)
        FRAuth.start(this)
    }
}