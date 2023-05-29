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
     */
    fun getInputStream(context: Context): InputStream

    /**
     * Retrieve the [OutputStream] of the KeyStore
     * @param context Application Context
     */
    fun getOutputStream(context: Context): OutputStream

    /**
     * Retrieve the Keystore Type, default to [KeyStore.getDefaultType]
     */
    fun getKeystoreType(): String {
        return KeyStore.getDefaultType()
    }

    fun delete(context: Context)

    fun exist(context: Context): Boolean

}