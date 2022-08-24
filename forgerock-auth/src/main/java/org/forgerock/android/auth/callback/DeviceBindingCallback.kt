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

    /**
     * The userId of the received use
     */
    private lateinit var userId: String
    /**
     * The challenge received from server
     */
    private lateinit var challenge: String
    /**
     * The authentication type of the journey
     */
    private lateinit var deviceBindingAuthenticationType: DeviceBindingAuthenticationType
    /**
     * The title to be displayed in biometric prompt
     */
    private lateinit var title: String
    /**
     * The subtitle to be displayed in biometric prompt
     */
    private lateinit var subtitle: String
    /**
     * The description to be displayed in biometric prompt
     */
    private lateinit var description: String
    /**
     * The timeout to be to expire the biometric authentication
     */
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

    /**
     * Input the JWS key to the server
     * @param value The JWS value.
     */
    fun setJws(value: String?) {
        super.setValue(value, 0)
    }

    /**
     * Input the Device Name to the server
     * @param value The device name value.
     */
    fun setDeviceName(value: String?) {
        super.setValue(value, 1)
    }

    /**
     * Input the Device Id to the server
     * @param value The device Id.
     */
    fun setDeviceId(value: String?) {
        super.setValue(value, 2)
    }

    /**
     * Input the Client Error to the server
     * @param value DeviceBind ErrorType .
     */
    fun setClientError(value: String?) {
        super.setValue(value, 3)
    }

    /**
     * Bind the device.
     *
     * @param context  The Application Context
     * @param listener The Listener to listen for the result
     */
    fun bind(context: Context,
             listener: FRListener<Void>) {
        execute(context, listener)
    }

    /**
     * Helper method to execute binding , signing, show biometric prompt.
     *
     * @param context  The Application Context
     * @param listener The Listener to listen for the result
     * @param authInterface Interface to find the Authentication Type
     * @param encryptedPreference Persist the values in encrypted shared preference
     */
    internal fun execute(context: Context,
                         listener: FRListener<Void>,
                         authInterface: Authenticator = getDeviceBindInterface(),
                         encryptedPreference: DeviceRepository = SharedPreferencesDeviceRepository(context),
                         deviceId: String = DeviceIdentifier.builder().context(context).build().identifier) {

        if(authInterface.isSupported().not()) {
            handleException(Unsupported(), listener = listener)
            return
        }

        try {
            val keypair = authInterface.generateKeys()
            authInterface.authenticate(timeout ?: 60) { result ->
                if (result is Success) {
                    val kid = encryptedPreference.persist(userId, keypair.keyAlias, deviceBindingAuthenticationType)
                    val jws = authInterface.sign(keypair, kid, userId, challenge)
                    setJws(jws)
                    setDeviceId(deviceId)
                    Listener.onSuccess(listener, null)
                } else {
                    // All the biometric exception is handled here , it could be Abort or timeout
                    handleException(result, listener = listener)
                }
            }
        }
        catch (e: Exception) {
            // This Exception happens only when there is Signing or keypair failed.
            handleException(Unsupported(errorMessage = e.message), listener = listener)
        }
    }

    /**
     * Handle all the errors for the device binding.
     *
     * @param status  DeviceBindingStatus(timeout,Abort, unsupported)
     * @param listener The Listener to listen for the result
     */
      open fun handleException(status: DeviceBindingStatus,
                             listener: FRListener<Void>) {

        setClientError(status.clientError)
        Logger.error(tag, status.message, status.errorCode)
        Listener.onException(
            listener,
            DeviceBindingException(status.message)
        )
    }

    /**
     * create the interface for the Authentication type(Biometric, Biometric_Fallback, none)
     */
    open fun getDeviceBindInterface(): Authenticator {
        return BindingFactory.getType(userId, deviceBindingAuthenticationType, title, subtitle, description)
    }
}

/**
 * convert authentication string received from server to authentication enum
 */
enum class DeviceBindingAuthenticationType constructor(val serializedValue: String?) {
    BIOMETRIC_ONLY("BIOMETRIC_ONLY"),
    BIOMETRIC_ALLOW_FALLBACK("BIOMETRIC_ALLOW_FALLBACK"),
    NONE("NONE");
}