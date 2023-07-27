package org.forgerock.android.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys


/**
 * create the encrypted shared preference for the given filename
 * @param context  The application context
 * @param fileName The default value is the secret_shared_prefs + (package name of the application)
 */
fun EncryptedPreferences.Companion.migrationFromBeta(
    context: Context,
    fileName: String = "secret_shared_prefs" + context.packageName,
    aliasName: String = fileName,
    sharedPreferences: SharedPreferences = context.getSharedPreferences("ORG_FORGEROCK_V_1_MIGRATION", Context.MODE_PRIVATE)
): SharedPreferences? {

    var newDataRepository: SharedPreferences? = null
    val alreadyMigrated = sharedPreferences.getBoolean("alreadyMigrated", false)

    if(alreadyMigrated.not()) {

        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        // Creates the instance for the encrypted preferences.
        val preferences = EncryptedSharedPreferences.create(
            fileName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val map = mutableMapOf<String, MutableSet<String>>()
        preferences.all.entries.map { it.key }.forEach { key ->
            preferences.getStringSet(key, emptySet())?.let {
                val result = mutableSetOf<String>()
                result.addAll(it)
                map[key] = result
            }
        }
        preferences.edit().clear().apply()
        deleteMasterKeyEntry(masterKeyAlias)
        deletePreferencesFile(context, fileName)

        newDataRepository =
            createPreferencesFile(context, fileName, aliasName)
        newDataRepository.edit().apply {
            map.forEach {
                this.putStringSet(it.key, it.value)
            }
        }.apply()

        sharedPreferences.edit()?.putBoolean("alreadyMigrated", true)?.apply()
    }

    return newDataRepository
}
