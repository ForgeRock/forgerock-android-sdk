/*
 * Copyright (c) 2019 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidVersionAwareTestRunner::class)
class DefaultSingleSignOnManagerTest : AndroidBaseTest() {
    private lateinit var tokenManager: SingleSignOnManager

    @Before
    fun setUp() {
        tokenManager = DefaultSingleSignOnManager.builder()
            .context(context).build()
    }

    @After
    @Throws(Exception::class)
    fun cleanup() {
        tokenManager.clear()
    }

    @Test
    fun storeToken() {
        val token = SSOToken("MyTokenValue")

        tokenManager.persist(token)

        val storedToken: SSOToken = tokenManager.token
        Assert.assertEquals("MyTokenValue", storedToken.value)
    }

    @Test
    fun clearSSOToken() {
        val token = SSOToken("MyTokenValue")
        tokenManager.persist(token)
        tokenManager.clear()

        val storedToken: SSOToken? = tokenManager.token
        Assert.assertNull(storedToken)
    }
}
