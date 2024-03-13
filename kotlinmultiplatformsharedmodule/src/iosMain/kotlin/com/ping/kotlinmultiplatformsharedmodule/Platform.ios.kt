package com.ping.kotlinmultiplatformsharedmodule
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ptr
import platform.UIKit.UIDevice
import platform.darwin.OS_LOG_DEFAULT
import platform.darwin.OS_LOG_TYPE_FAULT
import platform.darwin.OS_LOG_TYPE_INFO
import platform.darwin.__dso_handle
import platform.darwin._os_log_internal

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getLogger(): MFLoggerInterface = IOSMFLogger()

@OptIn(ExperimentalForeignApi::class)
class IOSMFLogger: MFLoggerInterface {


    @OptIn(ExperimentalForeignApi::class)
    override fun info(tag: String?, message: String?) {

        _os_log_internal(
            __dso_handle.ptr,
            OS_LOG_DEFAULT,
            OS_LOG_TYPE_INFO,
            message("I", tag, ("$message"))
        )
    }

    override fun network(tag: String?, message: String?) {
        _os_log_internal(
            __dso_handle.ptr,
            OS_LOG_DEFAULT,
            OS_LOG_TYPE_FAULT,
            message("N", tag,  ("$message "))
        )
    }

    override fun error(tag: String?, t: Throwable?, message: String?) {
        println("Error: $message")
        _os_log_internal(
            __dso_handle.ptr,
            OS_LOG_DEFAULT,
            OS_LOG_TYPE_FAULT,
            message("E", tag,  ("$message"), t)
        )
    }

    override fun error(tag: String?, message: String?) {
        _os_log_internal(
            __dso_handle.ptr,
            OS_LOG_DEFAULT,
            OS_LOG_TYPE_FAULT,
            message("E", tag,  ("$message"))
        )
    }

    override fun warn(tag: String?, message: String?) {
        _os_log_internal(
            __dso_handle.ptr,
            OS_LOG_DEFAULT,
            OS_LOG_TYPE_FAULT,
            message("W", tag,  ("$message"))
        )
    }

    override fun warn(tag: String?, t: Throwable?, message: String?) {
        _os_log_internal(
            __dso_handle.ptr,
            OS_LOG_DEFAULT,
            OS_LOG_TYPE_FAULT,
            message("W", tag,  ("$message"), t)
        )
    }

    override fun debug(tag: String?, message: String?) {
        _os_log_internal(
            __dso_handle.ptr,
            OS_LOG_DEFAULT,
            OS_LOG_TYPE_FAULT,
            message("D", tag,  ("$message"))
        )
    }

    private fun message(level: String, tag: String?, msg: String?, t: Throwable? = null): String {
        val str = if (tag?.isEmpty() == true) "$level: $msg" else "$level/$tag: $msg"
        return if (t == null) str else "$str $t"
    }

    override fun isNetworkEnabled(): Boolean {
        return MFLogger.isDebugEnabled()
    }
}