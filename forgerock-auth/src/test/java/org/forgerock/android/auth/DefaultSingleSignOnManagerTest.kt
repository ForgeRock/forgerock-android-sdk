/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultSingleSignOnManagerTest {

    private lateinit var tokenManager: SingleSignOnManager
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setUp() {
        sharedPreferences = context.getSharedPreferences("Dummy", Context.MODE_PRIVATE)
        tokenManager = DefaultSingleSignOnManager.builder()
            .sharedPreferences(sharedPreferences)
            .context(context).build()
    }

    @After
    fun cleanup() {
        sharedPreferences.edit().clear().commit()
        tokenManager.clear()
    }

    @Test
    fun storeToken() {
        val token = SSOToken("MyTokenValue")
        tokenManager.persist(token)
        val storedToken: Token = tokenManager.token
        Assert.assertEquals("MyTokenValue", storedToken.value)
    }

    @Test
    fun clearToken() {
        val token = SSOToken("MyTokenValue")
        tokenManager.persist(token)
        tokenManager.clear()
        val storedToken: Token? = tokenManager.token
        assertNull(storedToken)
    }
}