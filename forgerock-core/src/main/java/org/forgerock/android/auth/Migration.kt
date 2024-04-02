package org.forgerock.android.auth

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert
import java.io.File

class SharedPreferenceRepository {
    companion object {

        //1. Authenticator App

        //Alias to store keys
        private val ORG_FORGEROCK_SHARED_PREFERENCES_KEYS = "org.forgerock.android.authenticator.KEYS"
        //Settings to store the data
        private val ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT =
            "org.forgerock.android.authenticator.DATA.ACCOUNT"
        private val ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM =
            "org.forgerock.android.authenticator.DATA.MECHANISM"
        private val ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS =
            "org.forgerock.android.authenticator.DATA.NOTIFICATIONS"


        //2. Auth App

        //Alias to store keys
        private val ORG_FORGEROCK_V_1_KEYS = "org.forgerock.v1.KEYS"
        //File name to store AccessToken tokens
        private val ORG_FORGEROCK_V_1_TOKENS = "org.forgerock.v1.TOKENS"
        //File name to store SSO tokens
        private val ORG_FORGEROCK_V_1_SSO_TOKENS = "org.forgerock.v1.SSO_TOKENS"

        private val SSO_TOKEN = "org.forgerock.v1.SSO_TOKEN"
        private val COOKIES = "org.forgerock.v1.COOKIES"
        private val ACCESS_TOKEN = "access_token"

        private val TAG = SharedPreferenceRepository::class.java.simpleName

        @JvmStatic
        fun migrateToEncryptedSharedPref(context: Context) {
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                listOf(
                    Pair(ORG_FORGEROCK_V_1_TOKENS, ORG_FORGEROCK_V_1_KEYS),
                    Pair(ORG_FORGEROCK_V_1_SSO_TOKENS, ORG_FORGEROCK_V_1_KEYS)
                ).forEach {
                    migrate(context, it.first, it.second)
                }
            }
        }

        @JvmStatic
         fun migrateAuthenticatorToEncryptedSharedPref(context: Context) {
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                listOf(
                    Pair(
                        ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT,
                        ORG_FORGEROCK_SHARED_PREFERENCES_KEYS
                    ),
                    Pair(
                        ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM,
                        ORG_FORGEROCK_SHARED_PREFERENCES_KEYS
                    ),
                    Pair(
                        ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS,
                        ORG_FORGEROCK_SHARED_PREFERENCES_KEYS
                    )
                ).forEach {
                    migrateAuthenticator(context, it.first, it.second)
                }
            }
        }

        private fun migrateAuthenticator(context: Context, fileName: String, aliasName: String) {
            val dir = File(context.applicationInfo.dataDir, "shared_prefs")
            val file = File(dir, "$fileName.xml")

            if (file.exists().not()) {
                return
            }
            val oldSharedPreference = SecuredSharedPreferences(context, fileName, aliasName)
            val sharedPreference = EncryptedPreferences.getInstance(
                context,
                "$fileName.latest", aliasName
            )
            sharedPreference.edit().clear().apply()

            with(sharedPreference.edit()) {
                oldSharedPreference.all.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Int -> putInt(key, value)
                        is Boolean -> putBoolean(key, value)
                        is Float -> putFloat(key, value)
                        is Long -> putLong(key, value)
                        is Set<*> -> putStringSet(key, value as Set<String>)
                        else -> throw IllegalArgumentException("Unknown type")
                    }
                }
                apply()
            }
            deleteSharedPreferences(context, fileName)
            file.delete()

        }

        private fun migrate(context: Context, fileName: String, aliasName: String) {
            try {
                val dir = File(context.applicationInfo.dataDir, "shared_prefs")
                val file = File(dir, "$fileName.xml")

                if (file.exists().not()) {
                    return
                }
                val oldSharedPreference = SecuredSharedPreferences(context, fileName, aliasName)
                val sharedPreference = EncryptedPreferences.getInstance(
                    context,
                    "$fileName.latest", aliasName
                )
                sharedPreference.edit().clear().apply()

                with(sharedPreference.edit()) {
                    oldSharedPreference.getStringSet(COOKIES, null)?.let {
                        this.putStringSet(COOKIES, it)
                    }
                    oldSharedPreference.getString(SSO_TOKEN, null)?.let {
                        this.putString(SSO_TOKEN, it)
                    }
                    oldSharedPreference.getString(ACCESS_TOKEN, null)?.let {
                        this.putString(ACCESS_TOKEN, it)
                    }
                    this.apply()
                }
                deleteSharedPreferences(context, fileName)
                file.delete()

            } catch (e: Exception) {
                Logger.error(TAG, e.message)
            }
        }

        private fun deleteSharedPreferences(context: Context, name: String): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return context.deleteSharedPreferences(name)
            } else {
                context.getSharedPreferences(name, MODE_PRIVATE).edit().clear().apply()
                val dir = File(context.applicationInfo.dataDir, "shared_prefs")
                return File(dir, "$name.xml").delete()
            }
        }
    }
}