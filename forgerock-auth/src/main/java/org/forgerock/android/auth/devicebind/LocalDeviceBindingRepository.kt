/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.content.Context
import android.content.SharedPreferences
import org.forgerock.android.auth.EncryptedPreferences
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.forgerock.android.auth.exception.ApiException
import org.json.JSONObject
import java.io.IOException

const val ORG_FORGEROCK_V_1_DEVICE_REPO = "org.forgerock.v1.DEVICE_REPO"

interface DeviceBindingRepository {

    suspend fun persist(userKey: UserKey)

    suspend fun getAllKeys(): List<UserKey>

    @Throws(ApiException::class, IOException::class)
    suspend fun delete(userKey: UserKey)

}

const val idKey = "id"
const val userIdKey = "userId"
const val kidKey = "kid"
const val authTypeKey = "authType"
const val userNameKey = "username"
const val createdAtKey = "createdAt"

/**
 * Helper class to save and retrieve EncryptedMessage
 */
internal class LocalDeviceBindingRepository(context: Context,
                                            private val sharedPreferences: SharedPreferences =
                                         EncryptedPreferences.getInstance(context,
                                             ORG_FORGEROCK_V_1_DEVICE_REPO)) :
    DeviceBindingRepository {

    /**
     * Persist the data in encrypted shared preference
     */
    override suspend fun persist(userKey: UserKey) {
        sharedPreferences.edit().putString(userKey.id, userKey.asJSONObject().toString()).apply()
    }

    override suspend fun getAllKeys(): List<UserKey> =
        sharedPreferences.all?.mapNotNull {
            val json = JSONObject(it.value as String)
            UserKey(
                json.getString(idKey),
                json.getString(userIdKey),
                json.getString(userNameKey),
                json.getString(kidKey),
                DeviceBindingAuthenticationType.valueOf(json.getString(authTypeKey)),
                json.getLong(createdAtKey)
            )
        }?.toMutableList() ?: mutableListOf()

    override suspend fun delete(userKey: UserKey) {
        sharedPreferences.edit().remove(userKey.id).apply()
    }
}
