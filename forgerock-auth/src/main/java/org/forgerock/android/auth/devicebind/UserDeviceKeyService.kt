package org.forgerock.android.auth.devicebind

import android.content.Context
import android.os.Parcelable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import kotlinx.parcelize.Parcelize
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.json.JSONObject

interface UserKeyService {
    fun set(key: UserKey)
    fun getKeyStatus(userId: String?): KeyFoundStatus
    val userKeys: MutableList<UserKey>
    var callback: ((UserKey) -> (Unit))?
}

internal class UserDeviceKeyService(context: Context,
                          private val encryptedPreference: DeviceRepository = SharedPreferencesDeviceRepository(context)): UserKeyService {

    override lateinit var userKeys: MutableList<UserKey>
    override var callback: ((UserKey) -> (Unit))? = null

    private fun getAllUsers(): MutableList<UserKey>? {
        return encryptedPreference.getAllKeys()?.mapNotNull {
            val json = JSONObject(it.value as String)
            UserKey(
                json.getString(userIdKey),
                json.getString(kidKey),
                DeviceBindingAuthenticationType.valueOf(json.getString(authTypeKey)),
                it.key
            )
        }?.toMutableList()
    }

    override fun set(key: UserKey) {
        callback?.let {
            it(key)
        }
    }

    override fun getKeyStatus(userId: String?): KeyFoundStatus {

        val users: MutableList<UserKey>? = getAllUsers()

        if (userId.isNullOrEmpty().not()) {
            val key = users?.firstOrNull { it.userId == userId }
            return key?.let { SingleKeyFound(it) } ?: NoKeysFound
        }

        return users?.let {
            userKeys = it
            when (it.size) {
                0 -> NoKeysFound
                1 -> SingleKeyFound(it.first())
                else -> MultipleKeysFound(it)
            }
        } ?: NoKeysFound
    }
}

sealed class KeyFoundStatus
data class SingleKeyFound(val key: UserKey): KeyFoundStatus()
data class MultipleKeysFound(val keys: MutableList<UserKey>): KeyFoundStatus()
object NoKeysFound: KeyFoundStatus()

@Parcelize
data class UserKey(val userId: String, val kid: String, val authType: DeviceBindingAuthenticationType, val keyAlias: String):
    Parcelable