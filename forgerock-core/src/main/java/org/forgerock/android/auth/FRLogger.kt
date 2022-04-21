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

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor
import org.forgerock.android.core.BuildConfig


interface LoggerInterface {
    fun error(tag: String, t: Throwable?, message: String?, vararg values: Any?)
    fun error(tag: String, message: String?, vararg values: Any?)
    fun warn(tag: String, message: String?, vararg values: Any?)
    fun warn(tag: String, t: Throwable?, message: String?, vararg values: Any?)
    fun debug(tag: String, message: String?, vararg values: Any?)
    fun network(tag: String, message: String?, vararg values: Any?)
}

class FRLogger: LoggerInterface {

    companion object {
        private val level = Logger.Level.WARN
    }

    private fun log(level: Logger.Level, tag: String, t: Throwable?, message: String, vararg args: Any) {
        if (level.ordinal >= FRLogger.level.ordinal) {
            val value = String.format("[%s] [%s]: ", BuildConfig.VERSION_NAME, tag) + String.format(
                message,
                args
            )
            when (level) {
                Logger.Level.DEBUG -> {
                    Log.i(Logger.FORGE_ROCK, value)
                    return
                }
                Logger.Level.WARN -> {
                    Log.w(Logger.FORGE_ROCK, value, t)
                    return
                }
                Logger.Level.ERROR -> Log.e(Logger.FORGE_ROCK, value, t)
            }
        }
    }

    override fun error(tag: String, t: Throwable?, message: String?, vararg values: Any?) {
        log(Logger.Level.ERROR, tag, t, message ?: "", values)
    }

    override fun error(tag: String, message: String?, vararg values: Any?) {
        log(Logger.Level.ERROR, tag, null, message ?: "", values)
    }

    override fun warn(tag: String, message: String?, vararg values: Any?) {
        log(Logger.Level.WARN, tag, null, message ?: "", values)
    }

    override fun warn(tag: String, t: Throwable?, message: String?, vararg values: Any?) {
        log(Logger.Level.WARN, tag, t, message ?: "", values)
    }

    override fun debug(tag: String, message: String?, vararg values: Any?) {
        log(Logger.Level.DEBUG, tag, null, message ?: "", values)
    }

    override fun network(tag: String, message: String?, vararg values: Any?) {
        log(Logger.Level.DEBUG, tag, null, message ?: "", values)
    }
}