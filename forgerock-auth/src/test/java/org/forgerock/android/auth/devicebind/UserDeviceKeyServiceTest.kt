/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class UserDeviceKeyServiceTest {

    val context: Context = ApplicationProvider.getApplicationContext()
    private val encryptedPreference = mock<DeviceRepository>()

    @Test
    fun getAllUsersWithEmptyUserId() {
        val userList = mutableMapOf<String, Any>()
        userList["ZJgBS+bL9Di2Qh2In/zkHW1STMZ61m48mAAk4eSZM5w="] = "{\"userId\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\",\"username\":\"jey\",\"kid\":\"ba48e524-62ae-40df-a437-274f91b0df87\",\"authType\":\"BIOMETRIC_ALLOW_FALLBACK\"}"
        userList["J0gBS+bL9Di2Qh2In/zkHW1STMZ61m48mAAk4eSZM5w="] = "{\"userId\":\"id=stoyan,ou=user,dc=openam,dc=forgerock,dc=org\", \"username\":\"stoyan\",\"kid\":\"ba48e524-62ae-40df-a437-274f91b0df87\",\"authType\":\"BIOMETRIC_ALLOW_FALLBACK\"}"
        whenever(encryptedPreference.getAllKeys()).thenReturn(userList)
        val userKeyService = UserDeviceKeyService(context, encryptedPreference)
        val keyStatus = userKeyService.getKeyStatus("")
        assertTrue(keyStatus is MultipleKeysFound)
    }

    @Test
    fun getAllUsersWithNullUserId() {
        val userList = mutableMapOf<String, Any>()
        userList["ZJgBS+bL9Di2Qh2In/zkHW1STMZ61m48mAAk4eSZM5w="] = "{\"userId\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\", \"username\":\"jey\", \"kid\":\"ba48e524-62ae-40df-a437-274f91b0df87\",\"authType\":\"BIOMETRIC_ALLOW_FALLBACK\"}"
        userList["J0gBS+bL9Di2Qh2In/zkHW1STMZ61m48mAAk4eSZM5w="] = "{\"userId\":\"id=stoyan,ou=user,dc=openam,dc=forgerock,dc=org\",\"username\":\"jey\", \"kid\":\"ba48e524-62ae-40df-a437-274f91b0df87\",\"authType\":\"BIOMETRIC_ALLOW_FALLBACK\"}"
        whenever(encryptedPreference.getAllKeys()).thenReturn(userList)
        val userKeyService = UserDeviceKeyService(context, encryptedPreference)
        val keyStatus = userKeyService.getKeyStatus(null)
        assertTrue(keyStatus is MultipleKeysFound)
    }

    @Test
    fun getSingleUsersWithNullUserId() {
        val userList = mutableMapOf<String, Any>()
        userList["ZJgBS+bL9Di2Qh2In/zkHW1STMZ61m48mAAk4eSZM5w="] = "{\"userId\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\", \"username\":\"jey\", \"kid\":\"ba48e524-62ae-40df-a437-274f91b0df87\",\"authType\":\"BIOMETRIC_ALLOW_FALLBACK\"}"
        whenever(encryptedPreference.getAllKeys()).thenReturn(userList)
        val userKeyService = UserDeviceKeyService(context, encryptedPreference)
        val keyStatus = userKeyService.getKeyStatus(null)
        assertTrue(keyStatus is SingleKeyFound)
    }

    @Test
    fun getSingleUsersWithUserId() {
        val userList = mutableMapOf<String, Any>()
        userList["ZJgBS+bL9Di2Qh2In/zkHW1STMZ61m48mAAk4eSZM5w="] = "{\"userId\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\", \"username\":\"jey\", \"kid\":\"ba48e524-62ae-40df-a437-274f91b0df87\",\"authType\":\"BIOMETRIC_ALLOW_FALLBACK\"}"
        whenever(encryptedPreference.getAllKeys()).thenReturn(userList)
        val userKeyService = UserDeviceKeyService(context, encryptedPreference)
        val keyStatus = userKeyService.getKeyStatus("id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org")
        assertTrue(keyStatus is SingleKeyFound)
    }

    @Test
    fun getNoUserUsersWithUserId() {
        val userList = mutableMapOf<String, Any>()
        userList["ZJgBS+bL9Di2Qh2In/zkHW1STMZ61m48mAAk4eSZM5w="] = "{\"userId\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\",\"username\":\"jey\", \"kid\":\"ba48e524-62ae-40df-a437-274f91b0df87\",\"authType\":\"BIOMETRIC_ALLOW_FALLBACK\"}"
        whenever(encryptedPreference.getAllKeys()).thenReturn(userList)
        val userKeyService = UserDeviceKeyService(context, encryptedPreference)
        val keyStatus = userKeyService.getKeyStatus("id=mockjey,ou=user")
        assertTrue(keyStatus is NoKeysFound)
    }

    @Test
    fun getNoUserWithNullUserId() {
        val userList = mutableMapOf<String, Any>()
        whenever(encryptedPreference.getAllKeys()).thenReturn(userList)
        val userKeyService = UserDeviceKeyService(context, encryptedPreference)
        val keyStatus = userKeyService.getKeyStatus(null)
        assertTrue(keyStatus is NoKeysFound)
    }

    @Test
    fun getNoUserWithNullUserList() {
        whenever(encryptedPreference.getAllKeys()).thenReturn(null)
        val userKeyService = UserDeviceKeyService(context, encryptedPreference)
        val keyStatus = userKeyService.getKeyStatus(null)
        assertTrue(keyStatus is NoKeysFound)
    }
}