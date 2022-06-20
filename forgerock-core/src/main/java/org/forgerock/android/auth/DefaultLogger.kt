/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth
import android.util.Log
import org.forgerock.android.core.BuildConfig
import java.lang.String.format

/**
 * Defines the interface for handling log messages in the ForgeRock Android SDK.
 */
interface FRLogger {

    /**
     * Logs a message at ERROR level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param t An exception to log.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun error(tag: String?, t: Throwable?, message: String?, vararg values: Any?)

    /**
     * Logs a message at ERROR level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun error(tag: String?, message: String?, vararg values: Any?)

    /**
     * Logs a message at WARN level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun warn(tag: String?, message: String?, vararg values: Any?)

    /**
     * Logs a message at WARN level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param t An exception to log.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun warn(tag: String?, t: Throwable?, message: String?, vararg values: Any?)

    /**
     * Logs a message at DEBUG level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun debug(tag: String?, message: String?, vararg values: Any?)

    /**
     * Logs a message at INFO level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun info(tag: String?, message: String?, vararg values: Any?)

    /**
     * Logs an network message.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun network(tag: String?, message: String?, vararg values: Any?)

    /**
     * Check if the network calls details should be logged or not.
     * @return Whether or not to log network calls details.
     */
    fun isNetworkEnabled(): Boolean {
        return false
    }
}

internal class DefaultLogger: FRLogger {
    private fun log(
        level: Logger.Level,
        tag: String,
        t: Throwable?,
        message: String,
        vararg args: Any?
    ) {
            val value = try {
                format(
                    "[%s] [%s]: ",
                    BuildConfig.VERSION_NAME,
                    tag
                ) + format(message, *args)
            } catch (e: Exception) {
                ""
            }

            when (level) {
                Logger.Level.DEBUG -> {
                    Log.d(Logger.FORGE_ROCK, value)
                    return
                }
                Logger.Level.INFO -> {
                    Log.i(Logger.FORGE_ROCK, value)
                    return
                }
                Logger.Level.WARN -> {
                    Log.w(Logger.FORGE_ROCK, value, t)
                    return
                }
                Logger.Level.ERROR -> Log.e(Logger.FORGE_ROCK, value, t)
                else -> {

                }
            }
    }

    override fun error(tag: String?, t: Throwable?, message: String?, vararg values: Any?) {
        log(Logger.Level.ERROR, tag ?: "", t, message ?: "", *values)
    }

    override fun error(tag: String?, message: String?, vararg values: Any?) {
        log(Logger.Level.ERROR, tag ?: "", null, message ?: "", *values)
    }

    override fun warn(tag: String?, message: String?, vararg values: Any?) {
        log(Logger.Level.WARN, tag ?: "", null, message ?: "", *values)
    }


    override fun warn(tag: String?, t: Throwable?, message: String?, vararg values: Any?) {
        log(Logger.Level.WARN, tag ?: "", t, message ?: "", *values)
    }

    override fun debug(tag: String?, message: String?, vararg values: Any?) {
        log(Logger.Level.DEBUG, tag ?: "", null, message ?: "", *values)
    }

    override fun info(tag: String?, message: String?, vararg values: Any?) {
        log(Logger.Level.INFO, tag ?: "", null, message ?: "", *values)
    }

    override fun network(tag: String?, message: String?, vararg values: Any?) {
        log(Logger.Level.DEBUG, tag ?: "", null, message ?: "", *values)
    }

    override fun isNetworkEnabled(): Boolean {
        return Logger.isDebugEnabled()
    }
}