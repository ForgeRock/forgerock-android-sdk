/*
 * Copyright (c) 2023 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.forgerock.android.auth.storage.Storage
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

    private lateinit var ssoTokenStorage: Storage<SSOToken>
    private lateinit var cookiesStorage: Storage<Collection<String>>

    @Before
    fun setUpStorage() {
        ssoTokenStorage = sharedPreferencesStorage<SSOToken>(context = context,
            filename = "ssotoken",
            key = "ssotoken", cacheable = false)
        cookiesStorage = sharedPreferencesStorage<Collection<String>>(context = context,
            filename = "cookies",
            key = "cookies", cacheable = false)
        Options.init(Store(ssoTokenStorage = ssoTokenStorage, cookiesStorage = cookiesStorage))
        tokenManager = DefaultSingleSignOnManager.builder()
            .context(context).build()
    }

    @After
    fun cleanup() {
        ssoTokenStorage.delete()
        cookiesStorage.delete()
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
    fun clearToken() {
        val token = SSOToken("MyTokenValue")
        tokenManager.persist(token)
        tokenManager.clear()
        val storedToken: SSOToken? = tokenManager.token
        assertNull(storedToken)
    }
}