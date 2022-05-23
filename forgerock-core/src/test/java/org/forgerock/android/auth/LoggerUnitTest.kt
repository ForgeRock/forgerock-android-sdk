/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito

class LoggerUnitTest {
    @Test
    fun testCustomLogger() {
        var errorWithThrowableCalled = false
        var errorMethodCalled = false
        var warnMethodCalled = false
        var warnMethodWithThrowableCalled = false
        var debugMethodCalled = false
        var infoMethodCalled = false
        var networkMethodCalled = false

        class CustomLoggerLogger : FRLogger {
            override fun error(tag: String?, t: Throwable?, message: String?, vararg values: Any?) {
                errorWithThrowableCalled = true
            }

            override fun error(tag: String?, message: String?, vararg values: Any?) {
                errorMethodCalled = true
            }
            override fun warn(tag: String?, message: String?, vararg values: Any?) {
                warnMethodCalled = true
            }
            override fun warn(tag: String?, t: Throwable?, message: String?, vararg values: Any?) {
                warnMethodWithThrowableCalled = true
            }
            override fun debug(tag: String?, message: String?, vararg values: Any?) {
                debugMethodCalled = true
            }
            override fun info(tag: String?, message: String?, vararg values: Any?) {
                infoMethodCalled = true
            }
            override fun network(tag: String?, message: String?, vararg values: Any?) {
                networkMethodCalled = true
            }
        }
        Logger.setCustomLogger(CustomLoggerLogger())
        assertNotNull(Logger.getNetworkInterceptor())
        Logger.debug("", "")
        Logger.info("", "", "")
        Logger.error("", "", "")
        Logger.error("", Throwable("error"), "", "")
        Logger.network("", "", "")
        Logger.warn("", "", "")
        Logger.warn("", Throwable("warning"), "", "")

        assertTrue(errorWithThrowableCalled)
        assertTrue(errorMethodCalled)
        assertTrue(warnMethodCalled)
        assertTrue(warnMethodWithThrowableCalled)
        assertTrue(debugMethodCalled)
        assertTrue(infoMethodCalled)
        assertTrue(networkMethodCalled)
    }

    @Test
    fun testDefaultLogger() {
        val frLogger = Mockito.mock(FRLogger::class.java)
        val throwable = Throwable("errorOrWarning")
        Logger.frLogger = frLogger
        Logger.debug("debug", "debug", "debug")
        Mockito.verify(frLogger).debug("debug", "debug", "debug")
        Logger.info("info", "info", "info")
        Mockito.verify(frLogger).info("info", "info", "info")
        Logger.error("error", throwable, "error", "error")
        Mockito.verify(frLogger).error("error", throwable,"error", "error")
        Logger.warn("error", throwable, "error", "error")
        Mockito.verify(frLogger).warn("error", throwable,"error", "error")
    }

    @Test
    fun testDebugEnabled() {
        Logger.set(Logger.Level.DEBUG)
        assertTrue(Logger.isDebugEnabled())
    }

    @Test
    fun networkInterceptorIsNullByDefault() {
        Logger.set(Logger.Level.INFO)
        val interceptor = Logger.getNetworkInterceptor()
        assertNull(interceptor)
    }

    @Test
    fun networkInterceptorIsNotNullOnDebugMode() {
        Logger.set(Logger.Level.DEBUG)
        val interceptor = Logger.getNetworkInterceptor()
        assertNotNull(interceptor)
    }

    @Test
    fun verifyInterceptorNull() {
        Logger.set(Logger.Level.ERROR)
        assertNull(Logger.getNetworkInterceptor())
        Logger.setCustomLogger(null)
        assertNull(Logger.getNetworkInterceptor())
        Logger.set(Logger.Level.WARN)
        assertNull(Logger.getNetworkInterceptor())
        Logger.setCustomLogger(null)
        assertNull(Logger.getNetworkInterceptor())
    }
}