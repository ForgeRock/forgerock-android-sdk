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
        @JvmStatic
        internal var frLogger: FRLogger = DefaultLogger(level)
          private set

        /**
         * Set the Log Level to Info/Debug/Warning/Error/None. Default LogLevel is Warning
         *
         * @param level Setting this level will reset the logger to Default Logger. Custom Logger will be inactive
         */
        @JvmStatic
        fun set(level: Level) {
            CoreEventDispatcher.CLEAR_OKHTTP.notifyObservers()
            Logger.level = level
            frLogger = DefaultLogger(level)
        }

        /**
         * Set the Logger to Custom Logger
         *
         * @param logger Setting this logger will reset the logger to Custom Logger, Default Logger will be inactive
         */
        @JvmStatic
        fun setCustomLogger(logger: FRLogger) {
            CoreEventDispatcher.CLEAR_OKHTTP.notifyObservers()
            frLogger = logger
        }

        @JvmStatic
        fun isDebugEnabled(): Boolean {
             return level == Level.DEBUG
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