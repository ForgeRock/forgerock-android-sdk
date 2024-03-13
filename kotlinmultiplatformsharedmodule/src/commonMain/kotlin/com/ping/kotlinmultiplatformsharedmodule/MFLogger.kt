package com.ping.kotlinmultiplatformsharedmodule

interface MFLoggerInterface {
    /**
     * Logs a message at ERROR level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param t An exception to log.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun error(
        tag: String?,
        t: Throwable?,
        message: String?,
    )

    /**
     * Logs a message at ERROR level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun error(
        tag: String?,
        message: String?,
    )

    /**
     * Logs a message at WARN level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun warn(
        tag: String?,
        message: String?,
    )

    /**
     * Logs a message at WARN level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param t An exception to log.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun warn(
        tag: String?,
        t: Throwable?,
        message: String?,
    )

    /**
     * Logs a message at DEBUG level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun debug(
        tag: String?,
        message: String?,
    )

    /**
     * Logs a message at INFO level.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun info(
        tag: String?,
        message: String?,
    )

    /**
     * Logs an network message.
     * @param tag Used to identify the source of the log message where the log call occurs.
     * @param message The message to be logged.
     * @param values Additional arguments relevant to the log message.
     */
    fun network(
        tag: String?,
        message: String?,
    )

    /**
     * Check if the network calls details should be logged or not.
     * @return Whether or not to log network calls details.
     */
    fun isNetworkEnabled(): Boolean {
        return false
    }
}

expect fun getLogger(): MFLoggerInterface
