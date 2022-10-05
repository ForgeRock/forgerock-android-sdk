/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SharedPreferencesDeviceRepositoryTests {

    private val sharedPreferences = mock<SharedPreferences>()
    private val editor = mock<SharedPreferences.Editor>()
    val context: Context = ApplicationProvider.getApplicationContext()
    val result = "{\"userId\":\"userid\",\"username\":\"stoyan\",\"kid\":\"bfe1fe2a-66be-49a3-9550-8eb042773d17\",\"authType\":\"BIOMETRIC_ONLY\"}"

    @Before
    fun setUp() {
        whenever(sharedPreferences.getString("key", null)).thenReturn(null)
        whenever(sharedPreferences.edit()).thenReturn(editor)
        whenever(editor.putString("key", result)).thenReturn(editor)
        whenever(editor.apply()).thenAnswer { Unit }
    }

    @Test
    fun persistData() {
        val testObject = SharedPreferencesDeviceRepository(context, sharedPreferences = sharedPreferences, uuid = "bfe1fe2a-66be-49a3-9550-8eb042773d17")
        testObject.persist( "userid", "stoyan","key", DeviceBindingAuthenticationType.BIOMETRIC_ONLY)
        verify(editor).putString("key", result)
    }

    @Test
    fun getAllTheData() {
        val testObject = SharedPreferencesDeviceRepository(context, sharedPreferences = sharedPreferences, uuid = "bfe1fe2a-66be-49a3-9550-8eb042773d17")
        testObject.getAllKeys()
        verify(sharedPreferences).all
    }

}