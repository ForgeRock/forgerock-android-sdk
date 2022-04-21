/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package com.forgerock.kotlinapp

import android.app.Application
import org.forgerock.android.auth.FRAuth
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.LoggerInterface

class ForgeRockApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.set(Logger.Level.DEBUG)
        Logger.setCustomLogger(DDLogger())
        FRAuth.start(this)
    }
}

class DDLogger: LoggerInterface {
    override fun error(tag: String, t: Throwable?, message: String?, vararg values: Any?) {
       print("error")
    }

    override fun error(tag: String, message: String?, vararg values: Any?) {
        print("error")
    }

    override fun warn(tag: String, message: String?, vararg values: Any?) {
        print("warn")
    }

    override fun warn(tag: String, t: Throwable?, message: String?, vararg values: Any?) {
        print("warn throwable")
    }

    override fun debug(tag: String, message: String?, vararg values: Any?) {
        print("debug")
    }

    override fun network(tag: String, message: String?, vararg values: Any?) {
        print("network")
    }

}