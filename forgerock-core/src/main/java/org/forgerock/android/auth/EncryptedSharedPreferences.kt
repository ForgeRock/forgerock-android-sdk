/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.KeyStore


class EncryptedPreferences {
    companion object {

        private val tag = EncryptedPreferences::class.java.simpleName
        private const val androidKeyStore = "AndroidKeyStore"
        /**
         * create the encrypted shared preference for the given filename
         * @param context  The application context
         * @param fileName The default value is the secret_shared_prefs + (package name of the application)
         * @param aliasName The alias name can be passed to create a master key
         */
        @JvmOverloads
        fun getInstance(
            context: Context,
            fileName: String = "secret_shared_prefs" + context.packageName,
            aliasName: String = fileName): SharedPreferences {

            return try {
                // Creates or gets the key to encrypt and decrypt.
                createPreferencesFile(context, fileName, aliasName)
            } catch (e: Exception) {
                // This is the workaround code when the file got corrupted. Google should provide a fix.
                // Issue - https://github.com/google/tink/issues/535
                Logger.error(tag, e.message)
                val deleted = deletePreferencesFile(context, fileName)
                Logger.debug(tag, "Shared prefs file deleted: $deleted")
                deleteMasterKeyEntry(aliasName)
                createPreferencesFile(context, fileName, aliasName)
            }
        }

        private fun deleteMasterKeyEntry(masterKeyAlias: String) {
            KeyStore.getInstance(androidKeyStore).apply {
                load(null)
                deleteEntry(masterKeyAlias)
            }
        }

        private fun deletePreferencesFile(context: Context, fileName: String): Boolean {
           // Clear the content of the file
            context.getSharedPreferences(fileName, MODE_PRIVATE).edit().clear().apply()
            // Delete the file
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(fileName)
            } else {
                val dir = File(context.applicationInfo.dataDir, "shared_prefs")
                File(dir, "$fileName.xml").delete()
            }
        }

        // Creates the instance for the encrypted preferences.
        private fun createPreferencesFile(context: Context,
                                              fileName: String,
                                              aliasName: String): SharedPreferences {


            val masterKeyAlias = MasterKey.Builder(context, aliasName)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
