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

        open class CustomLogger : FRLogger {
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

            override fun isNetworkEnabled(): Boolean {
                return true
            }
        }

        open class CustomLoggerNetworkNotEnabled: CustomLogger() {
            override fun isNetworkEnabled(): Boolean {
                return false
            }
        }

        Logger.setCustomLogger(CustomLogger())
        Logger.set(Logger.Level.DEBUG)
        assertNotNull(Logger.frLogger)
        assertTrue(Logger.frLogger is CustomLogger)
        Logger.setCustomLogger(CustomLoggerNetworkNotEnabled())
        assertTrue(Logger.frLogger is CustomLoggerNetworkNotEnabled)
        assertNotNull(Logger.frLogger)

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
        assertFalse(networkMethodCalled)

        Logger.setCustomLogger(CustomLogger())
        Logger.network("", "", "")
        assertTrue(networkMethodCalled)

    }

    @Test
    fun testDefaultLogger() {
        val frLogger = Mockito.mock(FRLogger::class.java)
        val throwable = Throwable("errorOrWarning")
        Logger.setCustomLogger(frLogger)
        Logger.set(Logger.Level.DEBUG)
        assertNotNull(Logger.frLogger)
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
    fun testDefaultLoggerNotNull() {
        Logger.set(Logger.Level.DEBUG)
        val frLogger: FRLogger = DefaultLogger()
        Logger.setCustomLogger(frLogger)
        assertTrue(Logger.frLogger is DefaultLogger)
    }

}