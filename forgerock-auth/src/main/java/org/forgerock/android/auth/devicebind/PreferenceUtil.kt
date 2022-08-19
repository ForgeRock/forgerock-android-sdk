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

interface PreferenceInterface {
    fun persist(userId: String,
                key: String,
                authenticationType: DeviceBindingAuthenticationType): String
}

/**
 * Helper class to save and retrieve EncryptedMessage
 */
class PreferenceUtil(context: Context,
                     private val uuid: String = UUID.randomUUID().toString(),
                     private val sharedPreferences: SharedPreferences =
                         EncryptedPreferences.getInstance(context)): PreferenceInterface {

    /**
     * Saved and EncryptedMessage
     */
    override fun persist(userId: String,
                         key: String,
                         authenticationType: DeviceBindingAuthenticationType): String {
            val jsonObject = JSONObject()
            try {
                jsonObject.put("userId", userId)
                jsonObject.put("kid", uuid)
                jsonObject.put("authType", authenticationType.serializedValue)
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
            sharedPreferences.edit().putString(key, jsonObject.toString())?.apply()
        return this.uuid
    }
}

