package org.forgerock.android.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class EncryptedPreferences {
    companion object {
        fun getInstance(
            context: Context,
            masterKey: String = "forgerock_master"
        ): SharedPreferences {
            // Creates or gets the key to encrypt and decrypt.
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            // Creates the instance for the encrypted preferences.
            return EncryptedSharedPreferences.create(
                masterKey,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
