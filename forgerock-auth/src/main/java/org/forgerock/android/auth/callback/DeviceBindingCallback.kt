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

    private fun mapToServerError(error: DeviceBindingError) {
        if(error == DeviceBindingError.KeyCreationAndSign) {
            setClientError(DeviceBindingError.Unsupported.name)
        } else {
            setClientError(error.name)
        }
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

        if(authInterface.isSupported().not()) {
            handleException(DeviceBindingError.Unsupported, DeviceBindingError.Unsupported.message, listener = listener)
            return
        }

        try {
            val keypair = authInterface.generateKeys()
            authInterface.authenticate(timeout ?: 60) { result ->
                if (result.isSucceeded) {
                    val kid = encryptedPreference.persist(userId, keypair.keyAlias, deviceBindingAuthenticationType)
                    val jws = authInterface.sign(keypair, kid, userId, challenge)
                    setJws(jws)
                    setDeviceId(deviceId)
                    Listener.onSuccess(listener, null)
                } else {
                    // All the biometric exception is handled here , it could be Abort or timeout
                    handleException(result.errorType, result.errorMessage, result.errorCode, listener = listener)
                }
            }
        }
        catch (e: Exception) {
            // This  Exception happens only when there is Signing or keypair failed.
            handleException(DeviceBindingError.KeyCreationAndSign, e.message, listener = listener)
        }
    }

    open fun handleException(serverError: DeviceBindingError?,
                             logMessage: String?,
                             errorCode: Int? = null,
                             listener: FRListener<Void>) {
        serverError?.let {
            mapToServerError(it)
        }
        Logger.error(tag, logMessage)
        Listener.onException(
            listener,
            DeviceBindingException(logMessage, errorCode)
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