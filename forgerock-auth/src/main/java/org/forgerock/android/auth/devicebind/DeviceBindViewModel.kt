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
    fun getKeyStatus(userId: String?): KeyStatus
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
        return encryptedPreference.getAllUsers()?.mapNotNull {
            val json = JSONObject(it.value as String)
            UserKey(
                json.getString(userIdKey),
                json.getString(kidKey),
                DeviceBindingAuthenticationType.valueOf(json.getString(authTypeKey)),
                it.key
            )
        }?.toMutableList()
    }

    override fun getKeyStatus(userId: String?): KeyStatus {

        val users: MutableList<UserKey>? = getAllUsers()

        if (userId.isNullOrEmpty().not()) {
            val key = users?.firstOrNull { it.userId == userId }
            return key?.let { SingleKey(it) } ?: NoKey
        }

        return users?.let {
            userKeys = it
            when (it.size) {
                0 -> NoKey
                1 -> SingleKey(it.first())
                else -> MultipleKeys(it, InitProvider.getCurrentActivityAsFragmentActivity())
            }
        } ?: NoKey
    }
}

sealed class KeyStatus
data class SingleKey(val key: UserKey): KeyStatus()
data class MultipleKeys(val keys: MutableList<UserKey>, val activity: FragmentActivity): KeyStatus()
object NoKey: KeyStatus()

@Parcelize
data class UserKey(val userId: String, val kid: String, val authType: DeviceBindingAuthenticationType, val keyAlias: String):
    Parcelable