package com.ping.kotlinmultiplatformsharedmodule

import kotlin.jvm.JvmStatic

/**
 * Logger for ForgeRock SDK
 */
class MFLogger {
    companion object {
        const val FORGE_ROCK = "ForgeRock"

        // Default level to warn
        private var level = Level.WARN

        // Create Default Logger with Warning LogLevel
        @JvmStatic
        var frLogger: MFLoggerInterface = getLogger()
            private set

        /**
         * Set the Log Level to Info/Debug/Warning/Error/None. Default LogLevel is Warning.
         *
         * @param level The log level to be set.
         * @see Level
         *
         */
        @JvmStatic
        fun set(level: Level) {
            MFLogger.level = level
        }

        /**
         * Set the Logger to Custom Logger
         *
         * @param logger Setting this logger will reset the logger to Custom Logger, the Default Logger will become inactive.
         */
        @JvmStatic
        fun setCustomLogger(logger: MFLoggerInterface) {
            frLogger = logger
        }

        /**
         * Check if DEBUG Log Level is enabled
         * @return true if debug level is enabled and false otherwise
         */
        @JvmStatic
        fun isDebugEnabled(): Boolean {
            return level == Level.DEBUG
        }

        /**
         * Logs a message at ERROR level.
         * @param tag Used to identify the source of the log message where the log call occurs.
         * @param t An exception to log.
         * @param message The message to be logged.
         * @param values Additional arguments relevant to the log message.
         */
        @JvmStatic
        fun error(
            tag: String?,
            t: Throwable?,
            message: String?
        ) {
            verbosityCheck(Level.ERROR) {
                frLogger.error(tag, t, message)
            }
        }

        /**
         * Logs a message at ERROR level.
         * @param tag Used to identify the source of the log message where the log call occurs.
         * @param message The message to be logged.
         * @param values Additional arguments relevant to the log message.
         */
        @JvmStatic
        fun error(
            tag: String?,
            message: String?,
        ) {
            verbosityCheck(Level.ERROR) {
                frLogger.error(tag, message)
            }
        }

        /**
         * Logs a message at WARN level.
         * @param tag Used to identify the source of the log message where the log call occurs.
         * @param message The message to be logged.
         * @param values Additional arguments relevant to the log message.
         */
        @JvmStatic
        fun warn(
            tag: String?,
            message: String?,
        ) {
            verbosityCheck(Level.WARN) {
                frLogger.warn(tag, message)
            }
        }

        /**
         * Logs a message at WARN level.
         * @param tag Used to identify the source of the log message where the log call occurs.
         * @param t An exception to log.
         * @param message The message to be logged.
         * @param values Additional arguments relevant to the log message.
         */
        @JvmStatic
        fun warn(
            tag: String?,
            t: Throwable?,
            message: String?,
        ) {
            verbosityCheck(Level.WARN) {
                frLogger.warn(tag, t, message)
            }
        }

        /**
         * Logs a message at DEBUG level.
         * @param tag Used to identify the source of the log message where the log call occurs.
         * @param message The message to be logged.
         * @param values Additional arguments relevant to the log message.
         */
        @JvmStatic
        fun debug(
            tag: String?,
            message: String?
        ) {
            verbosityCheck(Level.DEBUG) {
                frLogger.debug(tag, message)
            }
        }

        /**
         * Logs a message at INFO level.
         * @param tag Used to identify the source of the log message where the log call occurs.
         * @param message The message to be logged.
         * @param values Additional arguments relevant to the log message.
         */
        @JvmStatic
        fun info(
            tag: String?,
            message: String?,
        ) {
            verbosityCheck(Level.INFO) {
                frLogger.info(tag, message)
            }
        }

        /**
         * Logs network call detail message.
         * @param tag Used to identify the source of the log message where the log call occurs.
         * @param message The message to be logged.
         * @param values Additional arguments relevant to the log message.
         */
        @JvmStatic
        fun network(
            tag: String?,
            message: String?,
        ) {
            if (frLogger.isNetworkEnabled()) {
                frLogger.network(tag, message)
            }
        }

        // checks the verbosityLevel to execute the logger
        private fun verbosityCheck(
            logLevel: Level,
            callback: () -> (Unit),
        ) {
            if (logLevel.ordinal >= level.ordinal) {
                callback()
            }
        }
    }

    /**
     * Log Level priority constants
     */
    enum class Level {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        NONE,
    }
}
