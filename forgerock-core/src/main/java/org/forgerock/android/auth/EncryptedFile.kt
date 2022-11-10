/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.File

class EncryptedFile {
    companion object {
        /**
         * create the encrypted shared preference for the given filename
         * @param context  The application context
         * @param fileName The default value is the secret_shared_prefs + (package name of the application)
         */
        fun getInstance(context: Context, file: File): EncryptedFile {
            // Creates or gets the key to encrypt and decrypt.
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            return EncryptedFile.Builder(file,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
        }
    }
}
