/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito

class InterceptorProviderTest {

    @Test
    fun testInterceptorWithInfoLevelLogger() {
        val logger: FRLogger = DefaultLogger()
        val provider = InterceptorProvider()
        val interceptor = provider.getInterceptor(logger)
        assertNull(interceptor)
    }

    @Test
    fun testInterceptorWithErrorLevelLogger() {
        val logger: FRLogger = DefaultLogger()
        val provider = InterceptorProvider()
        val interceptor = provider.getInterceptor(logger)
        assertNull(interceptor)
    }

    @Test
    fun testInterceptorWithWarnLevelLogger() {
        val logger: FRLogger = DefaultLogger()
        val provider = InterceptorProvider()
        val interceptor = provider.getInterceptor(logger)
        assertNull(interceptor)
    }

    @Test
    fun testInterceptorWithLoggerDebugModeButNetworkModeNotEnabled() {
        val logger: FRLogger = DefaultLogger()
        val provider = InterceptorProvider()
        val interceptor = provider.getInterceptor(logger)
        assertNotNull(interceptor)
        assertEquals(interceptor?.level, HttpLoggingInterceptor.Level.BODY)
    }

    @Test
    fun testInterceptorWithLoggerDebugModeButNetworkModeEnabledTrue() {
        Logger.set(Logger.Level.DEBUG)
        val provider = InterceptorProvider()
        val interceptor = provider.getInterceptor()
        assertNotNull(interceptor)
        assertEquals(interceptor?.level, HttpLoggingInterceptor.Level.BODY)
    }

    @Test
    fun testInterceptorWithCustomLogger() {
        val frLogger = Mockito.mock(FRLogger::class.java)
        Mockito.`when`(frLogger.isNetworkEnabled()).thenReturn(true)
        val provider = InterceptorProvider()
        val interceptor = provider.getInterceptor(frLogger)
        assertNotNull(interceptor)
        assertEquals(interceptor?.level, HttpLoggingInterceptor.Level.BODY)
    }
}