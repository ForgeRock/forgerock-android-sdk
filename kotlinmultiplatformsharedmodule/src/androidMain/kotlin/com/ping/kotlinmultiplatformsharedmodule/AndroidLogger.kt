package com.ping.kotlinmultiplatformsharedmodule

import android.util.Log
import java.lang.String.format

class AndroidMFDefaultLogger : MFLoggerInterface {
    private fun log(
        level: MFLogger.Level,
        tag: String,
        t: Throwable?,
        message: String,
    ) {
        val value =
            try {
                format(
                    "[%s] [%s]: ",
                    "BuildConfig.VERSION_NAME",
                    tag,
                ) + format(message)
            } catch (e: Exception) {
                ""
            }

        when (level) {
            MFLogger.Level.DEBUG -> {
                Log.d(MFLogger.FORGE_ROCK, value)
                return
            }

            MFLogger.Level.INFO -> {
                Log.i(MFLogger.FORGE_ROCK, value)
                return
            }

            MFLogger.Level.WARN -> {
                Log.w(MFLogger.FORGE_ROCK, value, t)
                return
            }

            MFLogger.Level.ERROR -> Log.e(MFLogger.FORGE_ROCK, value, t)
            else -> {
            }
        }
    }

    override fun error(
        tag: String?,
        t: Throwable?,
        message: String?,
    ) {
        log(MFLogger.Level.ERROR, tag ?: "", t, message ?: "")
    }

    override fun error(
        tag: String?,
        message: String?,
    ) {
        log(MFLogger.Level.ERROR, tag ?: "", null, message ?: "")
    }

    override fun warn(
        tag: String?,
        message: String?,
    ) {
        log(MFLogger.Level.WARN, tag ?: "", null, message ?: "")
    }

    override fun warn(
        tag: String?,
        t: Throwable?,
        message: String?,
    ) {
        log(MFLogger.Level.WARN, tag ?: "", t, message ?: "")
    }

    override fun debug(
        tag: String?,
        message: String?,
    ) {
        log(MFLogger.Level.DEBUG, tag ?: "", null, message ?: "")
    }

    override fun info(
        tag: String?,
        message: String?,
    ) {
        log(MFLogger.Level.INFO, tag ?: "", null, message ?: "")
    }

    override fun network(
        tag: String?,
        message: String?,
    ) {
        log(MFLogger.Level.DEBUG, tag ?: "", null, message ?: "")
    }

    override fun isNetworkEnabled(): Boolean {
        return MFLogger.isDebugEnabled()
    }
}

actual fun getLogger(): MFLoggerInterface = AndroidMFDefaultLogger()
