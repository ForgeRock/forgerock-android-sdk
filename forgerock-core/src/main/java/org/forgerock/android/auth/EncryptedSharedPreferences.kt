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
            aliasName: String = fileName
        ): SharedPreferences {

            return try {
                // Creates or gets the key to encrypt and decrypt.
                createPreferencesFile(context, fileName, aliasName)

            } catch (e: Exception) {
                Logger.error(tag, e.message)
                // This step is to recover keys for WebAuthN from beta4 to production. this is a throwaway code in future versions.
                val cache = recoverFromDefaultMasterKey(context, fileName)
                // This is the workaround code when the file got corrupted. Google should provide a fix.
                // Issue - https://github.com/google/tink/issues/535
                deleteKeyEntry(aliasName)
                val deleted = deletePreferencesFile(context, fileName)
                Logger.debug(tag, "Shared prefs file deleted: $deleted")
                val sharedPref = createPreferencesFile(context, fileName, aliasName)
                // This step is to add keys for new webauthn preference this is a throwaway code in future versions.
                sharedPref.edit().apply {
                    cache.forEach {
                        this.putStringSet(it.key, it.value)
                    }
                }.apply()
                return sharedPref
            }
        }

        private fun deleteKeyEntry(masterKeyAlias: String) {
            try {
                KeyStore.getInstance(androidKeyStore).apply {
                    load(null)
                    deleteEntry(masterKeyAlias)
                }
            }
            catch (e: Exception) {
                Logger.error(tag, e.message)
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
        private fun createPreferencesFile(
            context: Context,
            fileName: String,
            aliasName: String? = null
        ): SharedPreferences {
            val builder =
                aliasName?.let { MasterKey.Builder(context, it) } ?: MasterKey.Builder(context)
            builder.setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            return  EncryptedSharedPreferences.create(
                context,
                fileName,
                builder.build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        /**
         * This method is to extract the data from the sharedPreference which encrypted with MasterKey, and delete the master key.
         * @param context  The application context
         * @param fileName The default value is the secret_shared_prefs + (package name of the application)
         * @return The extracted data for the master key
         */

        private fun recoverFromDefaultMasterKey(
            context: Context,
            fileName: String = "secret_shared_prefs" + context.packageName
        ): MutableMap<String, MutableSet<String>> {
            val cache = mutableMapOf<String, MutableSet<String>>()
            try {
                val encryptedSharedPreferences =
                    createPreferencesFile(context, fileName)
                encryptedSharedPreferences.all.entries.map { it.key }.forEach { key ->
                    encryptedSharedPreferences.getStringSet(key, emptySet())?.let {
                        val result = mutableSetOf<String>()
                        result.addAll(it)
                        cache[key] = result
                    }
                }
                deleteKeyEntry("_androidx_security_master_key_")
            } catch (e: Exception) {
                Logger.error(tag, e.message)
            }
            return cache
        }
    }
}