/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import org.forgerock.android.auth.DeviceIdentifier
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Listener
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.devicebind.*
import org.json.JSONObject

/**
 * Callback to collect the device binding information
 */
open class DeviceBindingCallback: AbstractCallback {

    @JvmOverloads constructor(jsonObject: JSONObject,
                              index: Int): super(jsonObject, index)

    @JvmOverloads constructor(): super()

    private lateinit var userId: String
    private lateinit var challenge: String
    private lateinit var deviceBindingAuthenticationType: DeviceBindingAuthenticationType
    private lateinit var title: String
    private lateinit var subtitle: String
    private lateinit var description: String
    private var timeout: Int? = null
    private val tag = DeviceBindingCallback::class.java.simpleName

    override fun setAttribute(name: String, value: Any) = when (name) {
        "userId" ->  userId = value as String
        "challenge" -> challenge = value as String
        "authenticationType" -> deviceBindingAuthenticationType = DeviceBindingAuthenticationType.valueOf(value as String)
        "title" -> title = value as? String ?: ""
        "subtitle" -> subtitle = value as? String ?: ""
        "description" -> description = value as? String ?: ""
        "timeout" -> timeout = value as? Int
        else -> {}
    }

    override fun getType(): String {
        return "DeviceBindingCallback"
    }

    fun setJws(value: String?) {
        super.setValue(value, 0)
    }

    fun setDeviceName(value: String?) {
        super.setValue(value, 1)
    }

    fun setDeviceId(value: String?) {
        super.setValue(value, 2)
    }

    fun setClientError(value: String?) {
        super.setValue(value, 3)
    }

    fun bind(context: Context,
             listener: FRListener<Void>) {
        execute(context, listener)
    }

    internal fun execute(context: Context,
                         listener: FRListener<Void>,
                         authInterface: AuthenticationInterface = getAuthenticationType(),
                         encryptedPreference: PreferenceInterface = PreferenceUtil(context),
                         deviceId: String = DeviceIdentifier.builder().context(context).build().identifier) {

        when {
            authInterface.isSupported() -> {
                try {
                    val keypair = authInterface.generateKeys(context)
                    authInterface.authenticate(timeout ?: 60) { result ->
                        if (result.isSucceeded) {
                            val kid = encryptedPreference.persist(userId, keypair.keyAlias, deviceBindingAuthenticationType)
                            val jws = authInterface.sign(keypair, kid, userId, challenge)
                            setJws(jws)
                            setDeviceId(deviceId)
                            Listener.onSuccess(listener, null)
                        } else {
                            handleException(result.message, result.exception?.message ?: "", listener)
                        }
                    }
                }
                catch (e: Exception) {
                    handleException("Abort", e.message ?: "", listener)
                }
            }
            else -> {
                handleException("Unsupported", "Please verify the biometric or credential settings, we are unable to create key pair", listener)
            }
        }
    }

    private fun handleException(serverError: String,
                                logMessage: String,
                                listener: FRListener<Void>,) {
        setClientError(serverError)
        Logger.error(tag, logMessage)
        Listener.onException(
            listener,
            DeviceBindingException(logMessage)
        )
    }

    open fun getAuthenticationType(): AuthenticationInterface {
        return DeviceBindAuthentication.getType(userId, deviceBindingAuthenticationType, title, subtitle, description)
    }
}

enum class DeviceBindingAuthenticationType constructor(val serializedValue: String?) {
    BIOMETRIC_ONLY("BIOMETRIC_ONLY"),
    BIOMETRIC_ALLOW_FALLBACK("BIOMETRIC_ALLOW_FALLBACK"),
    NONE("NONE");
}