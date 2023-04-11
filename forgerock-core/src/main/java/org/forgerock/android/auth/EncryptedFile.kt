/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

class EncryptedFile {
    companion object {
        /**
         * create the encrypted shared preference for the given filename
         * @param context  The application context
         * @param file The default value is the secret_shared_prefs + (package name of the application)
         * @param aliasName The alias name can be passed to create a master key
         */
        fun getInstance(context: Context, file: File, aliasName: String): EncryptedFile {
            // Creates or gets the key to encrypt and decrypt.
            val masterKeyAlias = MasterKey.Builder(context, aliasName)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedFile.Builder(context, file,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
        }

    }
}

