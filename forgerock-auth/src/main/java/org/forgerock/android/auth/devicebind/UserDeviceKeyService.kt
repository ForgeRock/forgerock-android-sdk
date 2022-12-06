/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
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
    val userKeys: MutableList<UserKey>
}

internal class UserDeviceKeyService(context: Context,
                          private val encryptedPreference: DeviceRepository = SharedPreferencesDeviceRepository(context)): UserKeyService {

    /**
     * Get all the user keys in device.
     */
    override var userKeys: MutableList<UserKey> = mutableListOf()

    init {
          getAllUsers()
    }

    private fun getAllUsers() {
        encryptedPreference.getAllKeys()?.mapNotNull {
            val json = JSONObject(it.value as String)
            UserKey(
                json.getString(userIdKey),
                json.getString(userNameKey),
                json.getString(kidKey),
                DeviceBindingAuthenticationType.valueOf(json.getString(authTypeKey)),
                it.key
            )
        }?.toMutableList()?.also {
            userKeys = it
        }
    }

    /**
     * Fetch the key existence status in device.
     * @param  userId id optional and received from server
     */
    override fun getKeyStatus(userId: String?): KeyFoundStatus {
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
data class SingleKeyFound(val key: UserKey): KeyFoundStatus()
data class MultipleKeysFound(val keys: MutableList<UserKey>): KeyFoundStatus()
object NoKeysFound: KeyFoundStatus()

/**
 * UserKey DTO
 */
@Parcelize
data class UserKey(val userId: String,
                   val userName: String,
                   val kid: String,
                   val authType: DeviceBindingAuthenticationType,
                   val keyAlias: String): Parcelable
@Parcelize
data class UserKeys(
    val items: List<UserKey>?
) : Parcelable