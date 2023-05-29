/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.content.Context
import android.os.Parcelable
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import org.forgerock.android.auth.RemoteDeviceBindingRepository
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.forgerock.android.auth.callback.getAuthType
import org.json.JSONObject


interface UserKeyService {

    /**
     * Fetch the key existence status in device.
     * @param  userId id optional and received from server
     */
    fun getKeyStatus(userId: String?): KeyFoundStatus

    /**
     * Get all the user keys in device.
     */
    fun getAll(): List<UserKey>

    /**
     * Delete user key from local storage and also remotely from Server.
     * By default, if failed to delete from server, local storage will not be deleted,
     * by providing [forceDelete] to true, it will also delete local keys if server call is failed.
     *
     * @param userKey The [UserKey] to be deleted
     * @param forceDelete Default to false, true will delete local keys even server key removal is failed.
     */
    suspend fun delete(userKey: UserKey, forceDelete: Boolean = false)
}

internal class UserDeviceKeyService(val context: Context,
                                    private val remoteDeviceBindingRepository: DeviceBindingRepository = RemoteDeviceBindingRepository(),
                                    private val localDeviceBindingRepository: DeviceBindingRepository = LocalDeviceBindingRepository(
                                        context)) : UserKeyService {

    /**
     * Get all the user keys in device.
     */

    override fun getAll(): List<UserKey> = runBlocking {
        localDeviceBindingRepository.getAllKeys()
    }

    override suspend fun delete(userKey: UserKey, forceDelete: Boolean) {
        try {
            remoteDeviceBindingRepository.delete(userKey)
            deleteLocal(userKey)
        } catch (e: Exception) {
            if (forceDelete) {
                deleteLocal(userKey)
            }
            throw e
        }
    }

    private fun deleteLocal(userKey: UserKey) = runBlocking {
        localDeviceBindingRepository.delete(userKey)
        userKey.authType.getAuthType().initialize(userKey.userId).deleteKeys(context)
    }


    /**
     * Fetch the key existence status in device.
     * @param  userId id optional and received from server
     */
    override fun getKeyStatus(userId: String?): KeyFoundStatus {
        val userKeys = getAll()
        if (userId.isNullOrEmpty().not()) {
            val key = userKeys.firstOrNull { it.userId == userId }
            return key?.let {
                SingleKeyFound(it)
            } ?: NoKeysFound
        }

        return when (userKeys.size) {
            0 -> NoKeysFound
            1 -> SingleKeyFound(userKeys.first())
            else -> MultipleKeysFound(userKeys)
        }
    }
}

/**
 * Key existence status in device.
 */
sealed class KeyFoundStatus
data class SingleKeyFound(val key: UserKey) : KeyFoundStatus()
data class MultipleKeysFound(val keys: List<UserKey>) : KeyFoundStatus()
object NoKeysFound : KeyFoundStatus()

/**
 * UserKey DTO
 */
@Parcelize
data class UserKey internal constructor(
    val id: String,
    val userId: String,
    val userName: String,
    val kid: String,
    val authType: DeviceBindingAuthenticationType,
    val createdAt: Long = System.currentTimeMillis()) : Parcelable {

    fun asJSONObject(): JSONObject {
        val json = JSONObject()
        json.put(idKey, id)
        json.put(userIdKey, userId)
        json.put(userNameKey, userName)
        json.put(kidKey, kid)
        json.put(authTypeKey, authType.serializedValue)
        json.put(createdAtKey, createdAt)
        return json
    }
}

@Parcelize
data class UserKeys(
    val items: List<UserKey>?
) : Parcelable