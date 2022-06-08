/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import androidx.annotation.VisibleForTesting

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
        internal var frLogger: FRLogger = DefaultLogger()
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
            verbosityCheck(Level.ERROR) {
                frLogger.error(tag, t, message, *values)
            }
        }

        @JvmStatic
        fun error(tag: String?, message: String?, vararg values: Any?) {
            verbosityCheck(Level.ERROR) {
                frLogger.error(tag, message, *values)
            }
        }

        @JvmStatic
        fun warn(tag: String?, message: String?, vararg values: Any?) {
            verbosityCheck(Level.WARN) {
                frLogger.warn(tag, message, *values)
            }
        }

        @JvmStatic
        fun warn(tag: String?, t: Throwable?, message: String?, vararg values: Any?) {
            verbosityCheck(Level.WARN) {
                frLogger.warn(tag, t, message, *values)
            }
        }

        @JvmStatic
        fun debug(tag: String?, message: String?, vararg values: Any?) {
            verbosityCheck(Level.DEBUG) {
                frLogger.debug(tag, message, *values)
            }
        }

        @JvmStatic
        fun info(tag: String?, message: String?, vararg values: Any?) {
            verbosityCheck(Level.INFO) {
                frLogger.info(tag, message, *values)
            }
        }

        @JvmStatic
        fun network(tag: String?, message: String?, vararg values: Any?) {
            if(frLogger.isNetworkEnabled()) {
                frLogger.network(tag, message, *values)
            }
        }

        // checks the verbosityLevel to execute the logger
        private fun verbosityCheck(logLevel: Level, callback: ()->(Unit)) {
            if (logLevel.ordinal >= level.ordinal) {
                callback()
            }
        }
    }

    enum class Level {
        DEBUG, INFO, WARN, ERROR, NONE
    }
}