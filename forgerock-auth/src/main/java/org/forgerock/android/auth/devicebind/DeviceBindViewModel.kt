package org.forgerock.android.auth.devicebind

import android.content.Context
import android.os.Parcelable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import kotlinx.parcelize.Parcelize
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.json.JSONObject

interface ViewModelHandler {
    fun set(key: UserKey)
    fun getKeyStatus(userId: String?): KeyFound
    val userKeys: MutableList<UserKey>
    var callback: ((UserKey) -> (Unit))?
}

class DeviceBindViewModel(context: Context,
                          private val encryptedPreference: DeviceRepository = SharedPreferencesDeviceRepository(context)): ViewModel(), ViewModelHandler {

    override lateinit var userKeys: MutableList<UserKey>
    override var callback: ((UserKey) -> (Unit))? = null

    override fun set(key: UserKey) {
        callback?.let {
            it(key)
        }
    }

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

    override fun getKeyStatus(userId: String?): KeyFound {

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
                else -> MultipleKeysFound(it, InitProvider.getCurrentActivityAsFragmentActivity())
            }
        } ?: NoKeysFound
    }
}

sealed class KeyFound
data class SingleKeyFound(val key: UserKey): KeyFound()
data class MultipleKeysFound(val keys: MutableList<UserKey>, val activity: FragmentActivity): KeyFound()
object NoKeysFound: KeyFound()

@Parcelize
data class UserKey(val userId: String, val kid: String, val authType: DeviceBindingAuthenticationType, val keyAlias: String):
    Parcelable