/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import io.mockk.MockKAnnotations
import io.mockk.mockk
import io.mockk.verify
import org.forgerock.android.auth.callback.CallbackFactory
import org.junit.Before
import org.junit.Test

class PingOneProviderTest {

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }
    @Test
    fun testPauseBehaviourDataCalled() {

        val factory = mockk<CallbackFactory>(relaxed = true)
        val testObject = PingOneProvider()
        testObject.factory = factory
        testObject.onCreate()
        verify { factory.register(PingOneProtectInitializeCallback::class.java) }
        verify { factory.register(PingOneProtectEvaluationCallback::class.java) }
    }
}