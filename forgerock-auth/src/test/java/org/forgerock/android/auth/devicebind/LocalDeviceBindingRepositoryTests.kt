/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalDeviceBindingRepositoryTests {

    val context: Context = ApplicationProvider.getApplicationContext()
    private val sharedPreferences =
        context.getSharedPreferences("TestSharedPreferences", Context.MODE_PRIVATE)

    @After
    fun tearDown() {
        sharedPreferences.edit().clear().apply();
    }

    @Test
    fun persistData(): Unit = runBlocking {
        val testObject =
            LocalDeviceBindingRepository(context, sharedPreferences = sharedPreferences)
        val userKey = UserKey("id",
            "userid",
            "test",
            "kid",
            DeviceBindingAuthenticationType.BIOMETRIC_ONLY,
            123444)
        testObject.persist(userKey)
        val result = JSONObject(sharedPreferences.getString("id", null))
        assertThat(result.getString("id")).isEqualTo("id")
        assertThat(result.getString("userId")).isEqualTo("userid")
        assertThat(result.getString("username")).isEqualTo("test")
        assertThat(result.getString("kid")).isEqualTo("kid")
        assertThat(result.getLong("createdAt")).isEqualTo(123444)
        assertThat(result.getString("authType")).isEqualTo(DeviceBindingAuthenticationType.BIOMETRIC_ONLY.serializedValue)
    }

    @Test
    fun getAllTheData(): Unit = runBlocking {
        val testObject =
            LocalDeviceBindingRepository(context, sharedPreferences = sharedPreferences)
        val userKey1 = UserKey("id1",
            "userid",
            "test1",
            "kid",
            DeviceBindingAuthenticationType.BIOMETRIC_ONLY,
            123444)
        val userKey2 = UserKey("id2",
            "userid",
            "test2",
            "kid",
            DeviceBindingAuthenticationType.BIOMETRIC_ONLY,
            123444)
        testObject.persist(userKey1)
        testObject.persist(userKey2)

        assertThat(testObject.getAllKeys()).hasSize(2)
    }

}