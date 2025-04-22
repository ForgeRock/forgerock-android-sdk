/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.encrypt

import kotlinx.coroutines.runBlocking
import org.forgerock.android.auth.Encryptor

/**
 * A bridge to call [SuspendEncryptor] methods from a synchronous context.
 * @param encryptor The [SuspendEncryptor] to use for encryption and decryption.
 */
class EncryptorDelegate(private val encryptor: SuspendEncryptor) : Encryptor {

    override fun encrypt(clearText: ByteArray) = runBlocking {
        encryptor.encrypt(clearText)
    }

    override fun decrypt(encryptedData: ByteArray) = runBlocking {
        encryptor.decrypt(encryptedData)
    }

}