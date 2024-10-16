/*
 * Copyright (c) 2024. PingIdentity. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.encrypt

interface SuspendEncryptor {
    /**
     * Encrypts the given data.
     *
     * @param data The data to encrypt.
     *
     * @return The encrypted data.
     */
    suspend fun encrypt(data: ByteArray): ByteArray

    /**
     * Decrypts the given data.
     *
     * @param data The data to decrypt.
     *
     * @return The decrypted data.
     */
    suspend fun decrypt(data: ByteArray): ByteArray

}