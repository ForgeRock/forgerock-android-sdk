/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import org.forgerock.android.auth.encrypt.EncryptorDelegate
import org.forgerock.android.auth.encrypt.SecretKeyEncryptor

/**
 * The interface class [Encryptor] provides methods
 * to encrypt and decrypt data.
 */
interface Encryptor {
    /**
     * Encrypts the data.
     *
     * @param clearText bytes to encrypt
     * @return encrypted data
     */
    fun encrypt(clearText: ByteArray): ByteArray

    /**
     * Decrypts the data.
     *
     * @param encryptedData bytes to decrypt
     * @return decrypted data
     */
    fun decrypt(encryptedData: ByteArray): ByteArray

    companion object {
        @JvmStatic
        fun getEncryptor(context: Context, keyAlias: String): Encryptor {
            return EncryptorDelegate(SecretKeyEncryptor {
                this.context = context
                this.keyAlias = keyAlias
            })

        }
    }
}
