/*
 * Copyright (c) 2022 - 2023  ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.forgerock.android.auth.exception.ApiException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class UserDeviceKeyServiceTest {

    val context: Context = ApplicationProvider.getApplicationContext()
    private val remoteDeviceBindingRepository = mock<DeviceBindingRepository>()
    private lateinit var localDeviceBindingRepository: DeviceBindingRepository
    private lateinit var userKeyService: UserKeyService
    private val sharedPreferences =
        context.getSharedPreferences("TestSharedPreferences", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        localDeviceBindingRepository = LocalDeviceBindingRepository(context, sharedPreferences)
        userKeyService = UserDeviceKeyService(context, remoteDeviceBindingRepository, localDeviceBindingRepository)
    }

    @After
    fun tearDown() {
        sharedPreferences.edit().clear().apply();
    }


    @Test
    fun getAllUsersWithEmptyUserId() = runBlocking {
        val userKey1 = UserKey("id1",
            "user1",
            "user1",
            "kid1",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        val userKey2 = UserKey("id2",
            "user2",
            "user2",
            "kid2",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        localDeviceBindingRepository.persist(userKey1);
        localDeviceBindingRepository.persist(userKey2);
        val keyStatus = userKeyService.getKeyStatus("")
        assertTrue(keyStatus is MultipleKeysFound)
    }

    @Test
    fun getAllUsersWithNullUserId() = runBlocking {
        val userKey1 = UserKey("id1",
            "user1",
            "user1",
            "kid1",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        val userKey2 = UserKey("id2",
            "user2",
            "user2",
            "kid2",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        localDeviceBindingRepository.persist(userKey1);
        localDeviceBindingRepository.persist(userKey2);
        val keyStatus = userKeyService.getKeyStatus(null)
        assertTrue(keyStatus is MultipleKeysFound)
    }

    @Test
    fun getSingleUsersWithNullUserId() = runBlocking {
        val userKey1 = UserKey("id1",
            "user1",
            "user1",
            "kid1",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        localDeviceBindingRepository.persist(userKey1);
        val keyStatus = userKeyService.getKeyStatus(null)
        assertTrue(keyStatus is SingleKeyFound)
    }

    @Test
    fun getSingleUsersWithUserId() = runBlocking {
        val userKey1 = UserKey("id1",
            "user1",
            "userName1",
            "kid1",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        localDeviceBindingRepository.persist(userKey1);
        val keyStatus =
            userKeyService.getKeyStatus("user1")
        assertTrue(keyStatus is SingleKeyFound)
    }

    @Test
    fun getNoUserUsersWithUserId() = runBlocking {
        val userKey1 = UserKey("id1",
            "user1",
            "userName1",
            "kid1",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        localDeviceBindingRepository.persist(userKey1);

        val keyStatus = userKeyService.getKeyStatus("invalid")
        assertTrue(keyStatus is NoKeysFound)
    }

    @Test
    fun getNoUserWithNullUserId() {
        val keyStatus = userKeyService.getKeyStatus(null)
        assertTrue(keyStatus is NoKeysFound)
    }

    @Test
    fun testDelete(): Unit = runBlocking {
        val userKey1 = UserKey("id1",
            "user1",
            "user1",
            "kid1",
            DeviceBindingAuthenticationType.APPLICATION_PIN)
        val userKey2 = UserKey("id2",
            "user2",
            "user2",
            "kid2",
            DeviceBindingAuthenticationType.APPLICATION_PIN)
        localDeviceBindingRepository.persist(userKey1);
        localDeviceBindingRepository.persist(userKey2);

        var userKeys = userKeyService.getAll()
        assertThat(userKeys).hasSize(2)
        val userkey = userKeys[0]
        userKeyService.delete(userkey)
        userKeys = userKeyService.getAll()
        assertThat(userKeys).hasSize(1)

        //Delete a key which already deleted
        userKeyService.delete(userkey)
        userKeys = userKeyService.getAll()
        assertThat(userKeys).hasSize(1)

    }

    @Test
    fun testDeleteRemote(): Unit = runBlocking {
        val userKey1 = UserKey("id1",
            "user1",
            "user1",
            "kid1",
            DeviceBindingAuthenticationType.APPLICATION_PIN)
        localDeviceBindingRepository.persist(userKey1);

        var userKeys = userKeyService.getAll()
        assertThat(userKeys).hasSize(1)
        val userKey = userKeys[0]
        userKeyService.delete(userKey)
        userKeys = userKeyService.getAll()
        assertThat(userKeys).hasSize(0)
        verify(remoteDeviceBindingRepository).delete(userKey)

    }

    @Test
    fun `Test remote api failed to delete due to 403 return`(): Unit = runBlocking {
        whenever(remoteDeviceBindingRepository.delete(any())).thenThrow(ApiException(403,
            "Failed",
            "Failed"))
        val userKey1 = UserKey("id1",
            "user1",
            "user1",
            "kid1",
            DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
        localDeviceBindingRepository.persist(userKey1);

        var userKeys = userKeyService.getAll()
        assertThat(userKeys).hasSize(1)
        val userKey = userKeys[0]
        try {
            userKeyService.delete(userKey)
        } catch (e: Exception) {
            //ignore
        }
        userKeys = userKeyService.getAll()
        //user key should not be removed
        assertThat(userKeys).hasSize(1)
        verify(remoteDeviceBindingRepository).delete(userKey)

    }

    @Test
    fun `Test force delete when remote api failed to delete due to 403 return`(): Unit =
        runBlocking {
            whenever(remoteDeviceBindingRepository.delete(any())).thenThrow(ApiException(403,
                "Failed",
                "Failed"))
            val userKey1 = UserKey("id1",
                "user1",
                "user1",
                "kid1",
                DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK)
            localDeviceBindingRepository.persist(userKey1);

            var userKeys = userKeyService.getAll()
            assertThat(userKeys).hasSize(1)
            val userKey = userKeys[0]
            try {
                userKeyService.delete(userKey, true)
            } catch (e: Exception) {
                //ignore
            }
            userKeys = userKeyService.getAll()
            //user key should not be removed
            assertThat(userKeys).hasSize(0)
            verify(remoteDeviceBindingRepository).delete(userKey)

        }
}