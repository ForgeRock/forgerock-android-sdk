/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore

/**
 * Repository to store KeyStore
 */
interface KeyStoreRepository {

    /**
     * Retrieve the [InputStream] of the KeyStore
     * @param context Application Context
     * @param identifier KeyStore Identifier
     */
    fun getInputStream(context: Context, identifier: String): InputStream

    /**
     * Retrieve the [OutputStream] of the KeyStore
     * @param context Application Context
     * @param identifier KeyStore Identifier
     */
    fun getOutputStream(context: Context, identifier: String): OutputStream

    /**
     * Retrieve the Keystore Type, default to [KeyStore.getDefaultType]
     */
    fun getKeystoreType(): String {
        return KeyStore.getDefaultType()
    }

}