/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.forgerock.android.auth.ContextProvider
import org.forgerock.android.auth.Encryptor
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.SharedPreferencesSignOnManager

const val ORG_FORGEROCK_V_2_COOKIES = "org.forgerock.v2.COOKIES"
const val COOKIES = "org.forgerock.v2.COOKIES"
private const val TAG = "CookiesStorage"

/**
 * A storage class for managing cookies using secure shared preferences.
 *
 * @param context The application context.
 * @param filename The name of the file where cookies are stored.
 * @param keyAlias The alias for the encryption key.
 * @param key The key used to store the cookies.
 * @param serializer The serializer for the collection of cookies.
 * @param encryptor An optional encryptor for securing the cookies.
 */
class CookiesStorage(
    context: Context,
    filename: String,
    keyAlias: String,
    key: String,
    serializer: KSerializer<Collection<String>>,
    encryptor: Encryptor? = null
) : SecureSharedPreferencesStorage<Collection<String>>(
    context, filename, keyAlias, key, serializer, encryptor
) {

    init {
        val original = SharedPreferencesSignOnManager(context)
        try {
            original.cookies.let {
                if (it.isNotEmpty()) {
                    save(it)
                    original.clearCookies()
                }
            }
        } catch (e: Throwable) {
            Logger.error(TAG, e, "Failed to migrate cookies")
            original.clearCookies()
        }
    }
}

/**
 * Factory function to create a `CookiesStorage` instance.
 *
 * @param context The application context.
 * @param filename The name of the file where cookies are stored. Defaults to `ORG_FORGEROCK_V_2_COOKIES`.
 * @param keyAlias The alias for the encryption key. Defaults to `ORG_FORGEROCK_V_2_KEYS`.
 * @param key The key used to store the cookies. Defaults to `COOKIES`.
 * @return A `StorageDelegate` for `CookiesStorage`.
 */
fun CookiesStorage(
    context: Context = ContextProvider.context,
    filename: String = ORG_FORGEROCK_V_2_COOKIES,
    keyAlias: String = ORG_FORGEROCK_V_2_KEYS,
    key: String = COOKIES,
    encryptor: Encryptor? = null
): StorageDelegate<Collection<String>> =
    StorageDelegate {
        CookiesStorage(
            context = context,
            filename = filename,
            keyAlias = keyAlias,
            key = key,
            serializer = Json.serializersModule.serializer(),
            encryptor
        )
    }