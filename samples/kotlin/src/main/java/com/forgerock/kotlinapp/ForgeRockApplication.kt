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

class ForgeRockApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.set(Logger.Level.DEBUG)
        FRAuth.start(this)
    }
}