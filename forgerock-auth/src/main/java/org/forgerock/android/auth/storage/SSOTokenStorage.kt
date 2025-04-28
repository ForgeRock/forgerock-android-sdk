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
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.SSOToken
import org.forgerock.android.auth.SharedPreferencesSignOnManager

const val ORG_FORGEROCK_V_2_KEYS = "org.forgerock.v2.KEYS"
const val ORG_FORGEROCK_V_2_SSO_TOKENS = "org.forgerock.v2.SSO_TOKENS"
const val SSO_TOKEN = "org.forgerock.v2.SSO_TOKEN"
private const val TAG = "SSOTokenStorage"

/**
 * A storage class for managing SSO tokens using secure shared preferences.
 *
 * @param context The application context.
 * @param filename The name of the file where SSO tokens are stored.
 * @param keyAlias The alias for the encryption key.
 * @param key The key used to store the SSO tokens.
 * @param serializer The serializer for the SSO tokens.
 */
class SSOTokenStorage(
    context: Context,
    filename: String,
    keyAlias: String,
    key: String,
    serializer: KSerializer<SSOToken>
) : SecureSharedPreferencesStorage<SSOToken>(
    context, filename, keyAlias, key, serializer
) {

    init {
        val original = SharedPreferencesSignOnManager(context)
        try {
            original.token?.let {
                save(it)
                original.clearToken()
            }
        } catch (e: Throwable) {
            Logger.error(TAG, e, "Failed to migrate SSO token")
            original.clearToken()
        }
    }
}

/**
 * Factory function to create a `SSOTokenStorage` instance.
 *
 * @param context The application context.
 * @param filename The name of the file where SSO tokens are stored. Defaults to `ORG_FORGEROCK_V_2_SSO_TOKENS`.
 * @param keyAlias The alias for the encryption key. Defaults to `ORG_FORGEROCK_V_2_KEYS`.
 * @param key The key used to store the SSO tokens. Defaults to `SSO_TOKEN`.
 * @return A `StorageDelegate` for `SSOTokenStorage`.
 */
fun SSOTokenStorage(
    context: Context,
    filename: String = ORG_FORGEROCK_V_2_SSO_TOKENS,
    keyAlias: String = ORG_FORGEROCK_V_2_KEYS,
    key: String = SSO_TOKEN
): StorageDelegate<SSOToken> =
    StorageDelegate {
        SSOTokenStorage(
            context = context,
            filename = filename,
            keyAlias = keyAlias,
            key = key,
            serializer = Json.serializersModule.serializer(),
        )
    }