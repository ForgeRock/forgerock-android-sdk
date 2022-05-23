/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import androidx.annotation.VisibleForTesting
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Logger for ForgeRock SDK
 */
class Logger {

    companion object {

        @VisibleForTesting
        const val FORGE_ROCK = "ForgeRock"

        //Default level to warn
        private var level = Level.WARN

        //Create Default Logger with Warning LogLevel
        @VisibleForTesting
        internal var frLogger: FRLogger = DefaultLogger(level)

        //Create Default Interceptor
        private var interceptorProvider = InterceptorProvider(isDebugEnabled =  isDebugEnabled())

        @JvmStatic
        fun set(level: Level) {
            CoreEventDispatcher.CLEAR_OKHTTP.notifyObservers()
            Logger.level = level
            frLogger = DefaultLogger(level)
            interceptorProvider = InterceptorProvider(isDebugEnabled =  isDebugEnabled())
        }

        @JvmStatic
        fun setCustomLogger(logger: FRLogger?) {
            if (logger != null) {
                CoreEventDispatcher.CLEAR_OKHTTP.notifyObservers()
                frLogger = logger
                interceptorProvider = InterceptorProvider(customLogger = frLogger)
            }
        }

        @JvmStatic
        fun isDebugEnabled(): Boolean {
             return level == Level.DEBUG
        }

        @JvmStatic
        fun getNetworkInterceptor(): HttpLoggingInterceptor? {
            return interceptorProvider.getInterceptor()
        }

        @JvmStatic
        fun error(tag: String?, t: Throwable?, message: String?, vararg values: Any?) {
            frLogger.error(tag, t, message, *values)
        }

        @JvmStatic
        fun error(tag: String?, message: String?, vararg values: Any?) {
            frLogger.error(tag, message, *values)
        }

        @JvmStatic
        fun warn(tag: String?, message: String?, vararg values: Any?) {
            frLogger.warn(tag, message, *values)
        }

        @JvmStatic
        fun warn(tag: String?, t: Throwable?, message: String?, vararg values: Any?) {
            frLogger.warn(tag, t, message, *values)
        }

        @JvmStatic
        fun debug(tag: String?, message: String?, vararg values: Any?) {
            frLogger.debug(tag, message, *values)
        }

        @JvmStatic
        fun info(tag: String?, message: String?, vararg values: Any?) {
            frLogger.info(tag, message, *values)
        }

        @JvmStatic
        fun network(tag: String?, message: String?, vararg values: Any?) {
            frLogger.network(tag, message, *values)
        }
    }

    enum class Level {
        DEBUG, INFO, WARN, ERROR, NONE
    }
}