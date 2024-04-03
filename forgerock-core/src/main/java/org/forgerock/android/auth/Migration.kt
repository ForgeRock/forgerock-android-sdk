package org.forgerock.android.auth

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

object MigrationKeys {
    //1. Authenticator App

    //Alias to store keys
    const val ORG_FORGEROCK_SHARED_PREFERENCES_KEYS = "org.forgerock.android.authenticator.KEYS"
    //Settings to store the data
    const val ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT =
        "org.forgerock.android.authenticator.DATA.ACCOUNT"
    const val ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM =
        "org.forgerock.android.authenticator.DATA.MECHANISM"
    const val ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS =
        "org.forgerock.android.authenticator.DATA.NOTIFICATIONS"

    //2. Auth App

    //Alias to store keys
    const val ORG_FORGEROCK_V_1_KEYS = "org.forgerock.v1.KEYS"
    //File name to store AccessToken tokens
    const val ORG_FORGEROCK_V_1_TOKENS = "org.forgerock.v1.TOKENS"
    //File name to store SSO tokens
    const val ORG_FORGEROCK_V_1_SSO_TOKENS = "org.forgerock.v1.SSO_TOKENS"

    const val SSO_TOKEN = "org.forgerock.v1.SSO_TOKEN"
    const val COOKIES = "org.forgerock.v1.COOKIES"

    const val ACCESS_TOKEN = "access_token"

    //3. NewKeys

    const val NEW_ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT =
        "org.forgerock.android.authenticator.v2.DATA.ACCOUNT"
    const val NEW_ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM =
        "org.forgerock.android.authenticator.v2.DATA.MECHANISM"
    const val NEW_ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS =
        "org.forgerock.android.authenticator.v2.DATA.NOTIFICATIONS"

    //File name to store AccessToken tokens
    const val NEW_ORG_FORGEROCK_V_1_TOKENS = "org.forgerock.v2.TOKENS"
    //File name to store SSO tokens
    const val NEW_ORG_FORGEROCK_V_1_SSO_TOKENS = "org.forgerock.v2.SSO_TOKENS"


}
interface Migration {
    suspend fun migrate(context: Context,
                        sharedPreference: SharedPreferences,
                        oldSharedPreference: SecuredSharedPreferences) {

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
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
        }
    }

    fun deleteSharedPreferences(context: Context, name: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.deleteSharedPreferences(name)
        } else {
            context.getSharedPreferences(name, MODE_PRIVATE).edit().clear().apply()
            val dir = File(context.applicationInfo.dataDir, "shared_prefs")
            return File(dir, "$name.xml").delete()
        }
    }

    fun fileExists(context: Context, name: String): Boolean {
        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        return File(dir, "$name.xml").exists()
    }
}

class AccountMigration: Migration
class MechanismMigration: Migration
class NotificationMigration: Migration
class AuthMigration: Migration {
    override suspend fun migrate(context: Context,
                                 sharedPreference: SharedPreferences,
                                 oldSharedPreference: SecuredSharedPreferences) {
        with(sharedPreference.edit()) {
            oldSharedPreference.getString(MigrationKeys.ACCESS_TOKEN, null)?.let {
                this.putString(MigrationKeys.ACCESS_TOKEN, it)
            }
            this.apply()
        }

    }
}

class SSOMigration: Migration {
    override suspend fun migrate(context: Context,
                                 sharedPreference: SharedPreferences,
                                 oldSharedPreference: SecuredSharedPreferences) {

        with(sharedPreference.edit()) {
            oldSharedPreference.getStringSet(MigrationKeys.COOKIES, null)?.let {
                this.putStringSet(MigrationKeys.COOKIES, it)
            }
            oldSharedPreference.getString(MigrationKeys.SSO_TOKEN, null)?.let {
                this.putString(MigrationKeys.SSO_TOKEN, it)
            }
            this.apply()

        }
    }
}
interface MigrationStore {
    fun createMigration(): Migration
    fun createOldSharedPref(context: Context): SecuredSharedPreferences
    fun createNewSharedPref(context: Context): SharedPreferences
    fun getOldFileName(): String
    suspend fun migrate(context: Context) {
        val oldFileName = getOldFileName()
        val migration = createMigration()
        if(!migration.fileExists(context, oldFileName)) {
            return
        }
        val sharedPreference = createNewSharedPref(context)
        val oldSharedPreference = createOldSharedPref(context)
        migration.migrate(context, sharedPreference, oldSharedPreference)
        migration.deleteSharedPreferences(context, oldFileName)
    }
}

class AccountMigrationStore: MigrationStore {
    override fun createMigration(): Migration {
        return AccountMigration()
    }
    override  fun getOldFileName(): String {
        return MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT
    }
    override fun createOldSharedPref(context: Context): SecuredSharedPreferences {
        return SecuredSharedPreferences(
            context,
            MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT,
            MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_KEYS
        )
    }
    override fun createNewSharedPref(context: Context): SharedPreferences {
        return EncryptedPreferences.getInstance(
            context,
            MigrationKeys.NEW_ORG_FORGEROCK_SHARED_PREFERENCES_DATA_ACCOUNT,
            MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_KEYS
        )
    }
}

class MechanismMigrationStore: MigrationStore {
    override fun createMigration(): Migration {
        return MechanismMigration()
    }
    override  fun getOldFileName(): String {
        return MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM
    }
    override fun createOldSharedPref(context: Context): SecuredSharedPreferences {
        return SecuredSharedPreferences(
            context,
            MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM,
            MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_KEYS
        )
    }
    override fun createNewSharedPref(context: Context): SharedPreferences {
        return EncryptedPreferences.getInstance(
            context,
            MigrationKeys.NEW_ORG_FORGEROCK_SHARED_PREFERENCES_DATA_MECHANISM,
            MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_KEYS
        )
    }
}

class NotificationMigrationStore: MigrationStore {
    override fun createMigration(): Migration {
        return NotificationMigration()
    }

    override fun getOldFileName(): String {
        return MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS
    }
    override fun createOldSharedPref(context: Context): SecuredSharedPreferences  {
        return SecuredSharedPreferences(
            context,
            MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS,
            MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_KEYS
        )
    }
    override fun createNewSharedPref(context: Context): SharedPreferences {
        return EncryptedPreferences.getInstance(
            context,
            MigrationKeys.NEW_ORG_FORGEROCK_SHARED_PREFERENCES_DATA_NOTIFICATIONS,
            MigrationKeys.ORG_FORGEROCK_SHARED_PREFERENCES_KEYS
        )
    }
}

class AuthMigrationStore: MigrationStore {
    override fun createMigration(): Migration {
        return AuthMigration()
    }
    override fun getOldFileName(): String {
        return MigrationKeys.ORG_FORGEROCK_V_1_TOKENS
    }
    override fun createOldSharedPref(context: Context): SecuredSharedPreferences {
        return SecuredSharedPreferences(
            context,
            MigrationKeys.ORG_FORGEROCK_V_1_TOKENS,
            MigrationKeys.ORG_FORGEROCK_V_1_KEYS
        )
    }
    override fun createNewSharedPref(context: Context): SharedPreferences {
        return EncryptedPreferences.getInstance(
            context,
            MigrationKeys.NEW_ORG_FORGEROCK_V_1_TOKENS,
            MigrationKeys.ORG_FORGEROCK_V_1_KEYS
        )
    }
}

class SSOMigrationStore: MigrationStore {
    override fun createMigration(): Migration {
        return SSOMigration()
    }

    override fun getOldFileName(): String {
        return MigrationKeys.ORG_FORGEROCK_V_1_SSO_TOKENS
    }
    override fun createOldSharedPref(context: Context): SecuredSharedPreferences {
        return SecuredSharedPreferences(
            context,
            MigrationKeys.ORG_FORGEROCK_V_1_SSO_TOKENS,
            MigrationKeys.ORG_FORGEROCK_V_1_KEYS
        )
    }
    override fun createNewSharedPref(context: Context): SharedPreferences {
        return EncryptedPreferences.getInstance(
            context,
            MigrationKeys.NEW_ORG_FORGEROCK_V_1_SSO_TOKENS,
            MigrationKeys.ORG_FORGEROCK_V_1_KEYS
        )
    }
}

class MigrationManager {
    companion object {
        @JvmStatic
        fun orchestrate(
            context: Context,
            migrationStores: List<MigrationStore> = emptyList()) {
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                val deferList = mutableListOf<Deferred<Unit>>()
                migrationStores.forEach {
                    deferList.add(async {  it.migrate(context) })
                }
                deferList.awaitAll()
            }
        }
    }
}
