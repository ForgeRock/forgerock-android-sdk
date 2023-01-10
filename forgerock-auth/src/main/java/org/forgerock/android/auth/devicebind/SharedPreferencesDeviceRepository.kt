/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.content.Context
import android.content.SharedPreferences
import org.forgerock.android.auth.EncryptedPreferences
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.json.JSONException
import org.json.JSONObject
import java.util.*

const val ORG_FORGEROCK_V_1_DEVICE_REPO = "org.forgerock.v1.DEVICE_REPO"

interface DeviceRepository {
    /**
     * Persist the data in encrypted shared preference
     */
    fun persist(userId: String,
                userName: String,
                key: String,
                authenticationType: DeviceBindingAuthenticationType): String

    fun getAllKeys(): MutableMap<String, *>?

    fun delete(key: String)

}

const val userIdKey = "userId"
const val kidKey = "kid"
const val authTypeKey = "authType"
const val userNameKey = "username"

/**
 * Helper class to save and retrieve EncryptedMessage
 */
internal class SharedPreferencesDeviceRepository(context: Context,
                                        private val uuid: String = UUID.randomUUID().toString(),
                                        private val sharedPreferences: SharedPreferences =
                         EncryptedPreferences.getInstance(context, ORG_FORGEROCK_V_1_DEVICE_REPO)): DeviceRepository {

    /**
     * Persist the data in encrypted shared preference
     */
    override fun persist(userId: String,
                         userName: String,
                         key: String,
                         authenticationType: DeviceBindingAuthenticationType): String {
            val jsonObject = JSONObject()
            try {
                jsonObject.put(userIdKey, userId)
                jsonObject.put(userNameKey, userName)
                jsonObject.put(kidKey, uuid)
                jsonObject.put(authTypeKey, authenticationType.serializedValue)
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
            sharedPreferences.edit().putString(key, jsonObject.toString())?.apply()
        return this.uuid
    }

    override fun getAllKeys(): MutableMap<String, *>? {
        return sharedPreferences.all
    }

    override fun delete(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
}

