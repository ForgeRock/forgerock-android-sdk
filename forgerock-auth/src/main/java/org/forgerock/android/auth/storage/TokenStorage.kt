/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.forgerock.android.auth.AccessToken
import org.forgerock.android.auth.OAuth2

//Alias to store keys
const val ORG_FORGEROCK_V_1_KEYS = "org.forgerock.v1.KEYS"
//File name to store tokens
const val ORG_FORGEROCK_V_1_TOKENS = "org.forgerock.v1.TOKENS"

/**
 * Create a StorageDelegate for AccessToken using SecureSharedPreferencesStorage.
 *
 * @param context The application context.
 * @param filename The name of the file where tokens are stored.
 * @param keyAlias The alias for the encryption key.
 * @param key The key used to store the tokens.
 * @return A StorageDelegate for AccessToken.
 */
fun TokenStorage(
    context: Context,
    filename: String = ORG_FORGEROCK_V_1_TOKENS,
    keyAlias: String = ORG_FORGEROCK_V_1_KEYS,
    key: String = OAuth2.ACCESS_TOKEN
): StorageDelegate<AccessToken> =
    StorageDelegate {
        SecureSharedPreferencesStorage(
            context = context,
            filename = filename,
            keyAlias = keyAlias,
            key = key,
            serializer = Json.serializersModule.serializer(),
        )
    }