package org.forgerock.android.auth

import android.content.Context

object FRApplicationContext {

    lateinit var applicationInstance: Context
        private set

    fun initContext(context: Context) {
        applicationInstance = context.applicationContext;
    }
}