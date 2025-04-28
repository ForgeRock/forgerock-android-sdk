/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import org.forgerock.android.auth.SecuredSharedPreferences
import org.forgerock.android.auth.json

/**
 * A storage class for managing data using secure shared preferences.
 *
 * @param T The type of object to be stored.
 * @param context The application context.
 * @param filename The name of the file where data is stored.
 * @param keyAlias The alias for the encryption key.
 * @param key The key used to store the data.
 * @param serializer The serializer for the data.
 */
open class SecureSharedPreferencesStorage<T : @Serializable Any>(
    context: Context,
    filename: String,
    keyAlias: String,
    private var key: String,
    private val serializer: KSerializer<T>,
) : Storage<T> {

    private var sharedPreferences: SharedPreferences =
        SecuredSharedPreferences(context, filename, keyAlias)

    /**
     * Save an item to the secure shared preferences storage.
     *
     * @param item The item to be saved.
     */
    override fun save(item: T) {
        sharedPreferences.edit().putString(key, json.encodeToString(serializer, item)).commit()
    }

    /**
     * Retrieve an item from the secure shared preferences storage.
     *
     * @return The retrieved item, or null if no item is found.
     */
    override fun get(): T? {
        return sharedPreferences.getString(key, null)?.let {
            return json.decodeFromString(serializer, it)
        }
    }

    /**
     * Delete an item from the secure shared preferences storage.
     */
    override fun delete() {
        sharedPreferences.edit().remove(key).commit()
    }
}