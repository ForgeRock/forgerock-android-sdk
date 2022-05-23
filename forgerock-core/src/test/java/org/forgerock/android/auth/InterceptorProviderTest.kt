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


class InterceptorProviderTest {

    @Test
    fun testInterceptorWithLogger() {
        val logger: FRLogger = DefaultLogger(Logger.Level.INFO)
        val provider = InterceptorProvider(logger)
        val interceptor = provider.getInterceptor()
        assertNotNull(interceptor)
        assertEquals(interceptor?.level, HttpLoggingInterceptor.Level.BODY)
    }

    @Test
    fun testInterceptorWithLoggerAndDebugFalse() {
        val logger: FRLogger = DefaultLogger(Logger.Level.INFO)
        val provider = InterceptorProvider(logger, false)
        val interceptor = provider.getInterceptor()
        assertNotNull(interceptor)
        assertEquals(interceptor?.level, HttpLoggingInterceptor.Level.BODY)
    }

    @Test
    fun testInterceptorWithDebugEnabled() {
        val provider = InterceptorProvider(isDebugEnabled = true)
        val interceptor = provider.getInterceptor()
        assertNotNull(interceptor)
        assertEquals(interceptor?.level, HttpLoggingInterceptor.Level.BODY)
    }

    @Test
    fun testInterceptorWithNotDebugEnabled() {
        val provider = InterceptorProvider(null, isDebugEnabled = false)
        val interceptor = provider.getInterceptor()
        assertNull(interceptor)
    }
}