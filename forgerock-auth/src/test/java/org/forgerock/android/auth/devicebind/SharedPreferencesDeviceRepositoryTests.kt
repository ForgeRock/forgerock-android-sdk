/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SharedPreferencesDeviceRepositoryTests {

    val context: Context = ApplicationProvider.getApplicationContext()
    private val sharedPreferences = context.getSharedPreferences("TestSharedPreferences", Context.MODE_PRIVATE)

    val result = "{\"userId\":\"userid\",\"username\":\"stoyan\",\"kid\":\"bfe1fe2a-66be-49a3-9550-8eb042773d17\",\"authType\":\"BIOMETRIC_ONLY\",\"createdAt\":13123213213}"

    @After
    fun tearDown() {
        sharedPreferences.edit().clear().apply();
    }

    @Test
    fun persistData() {
        val testObject = SharedPreferencesDeviceRepository(context, sharedPreferences = sharedPreferences)
        testObject.persist( "userid", "stoyan","key", DeviceBindingAuthenticationType.BIOMETRIC_ONLY, 123444)
        val result = JSONObject(sharedPreferences.getString("key", null))
        assertThat(result.getString("userId")).isEqualTo("userid")
        assertThat(result.getString("username")).isEqualTo("stoyan")
        assertThat(result.getString("kid")).isNotNull
        assertThat(result.getLong("createdAt")).isEqualTo(123444)
        assertThat(result.getString("authType")).isEqualTo(DeviceBindingAuthenticationType.BIOMETRIC_ONLY.serializedValue)

    }

    @Test
    fun getAllTheData() {
        val testObject = SharedPreferencesDeviceRepository(context, sharedPreferences = sharedPreferences)
        testObject.persist( "userid", "test1","key1", DeviceBindingAuthenticationType.BIOMETRIC_ONLY)
        testObject.persist( "userid", "test2","key2", DeviceBindingAuthenticationType.APPLICATION_PIN)

        assertThat(testObject.getAllKeys()).hasSize(2)
    }

}