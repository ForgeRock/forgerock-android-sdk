/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.forgerock.android.auth.DeviceIdentifier
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Listener
import org.forgerock.android.auth.devicebind.ApplicationPinDeviceAuthenticator
import org.forgerock.android.auth.devicebind.BiometricAndDeviceCredential
import org.forgerock.android.auth.devicebind.BiometricOnly
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.*
import org.forgerock.android.auth.devicebind.DeviceBindingException
import org.forgerock.android.auth.devicebind.DeviceBindingStatus
import org.forgerock.android.auth.devicebind.DeviceRepository
import org.forgerock.android.auth.devicebind.KeyPair
import org.forgerock.android.auth.devicebind.None
import org.forgerock.android.auth.devicebind.Prompt
import org.forgerock.android.auth.devicebind.SharedPreferencesDeviceRepository
import org.forgerock.android.auth.devicebind.Success
import org.forgerock.android.auth.devicebind.initialize
import org.json.JSONObject

/**
 * Callback to collect the device binding information
 */
open class DeviceBindingCallback : AbstractCallback, Binding {

    @JvmOverloads
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)

    @JvmOverloads
    constructor() : super()

    /**
     * The userId received from server
     */
    lateinit var userId: String
        private set

    /**
     * The userName received from server
     */
    lateinit var userName: String
        private set

    /**
     * The challenge received from server
     */
    lateinit var challenge: String
        private set

    /**
     * The authentication type of the journey
     */
    lateinit var deviceBindingAuthenticationType: DeviceBindingAuthenticationType
        private set

    /**
     * The title to be displayed in biometric prompt
     */
    lateinit var title: String
        private set

    /**
     * The subtitle to be displayed in biometric prompt
     */
    lateinit var subtitle: String
        private set

    /**
     * The description to be displayed in biometric prompt
     */
    lateinit var description: String
        private set

    /**
     * The timeout to be to expire the biometric authentication
     */
    var timeout: Int? = null
        private set

    final override fun setAttribute(name: String, value: Any) = when (name) {
        "userId" -> userId = value as String
        "username" -> userName = value as String
        "challenge" -> challenge = value as String
        "authenticationType" -> deviceBindingAuthenticationType =
            DeviceBindingAuthenticationType.valueOf(value as String)
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
    override fun setClientError(value: String?) {
        super.setValue(value, 3)
    }

    /**
     * Bind the device.
     *
     * @param context  The Application Context
     * @param deviceAuthenticator A function to return a [DeviceAuthenticator], [deviceAuthenticatorIdentifier] will be used if not provided
     * @param listener The Listener to listen for the result
     */
    @JvmOverloads
    open fun bind(context: Context,
                  deviceAuthenticator: (type: DeviceBindingAuthenticationType) -> DeviceAuthenticator = deviceAuthenticatorIdentifier,
                  listener: FRListener<Void>) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                bind(context, deviceAuthenticator)
                Listener.onSuccess(listener, null)
            } catch (e: Exception) {
                Listener.onException(listener, e)
            }
        }
    }

    /**
     * Bind the device.
     *
     * @param context  The Application Context
     * @param deviceAuthenticator A function to return a [DeviceAuthenticator], [deviceAuthenticatorIdentifier] will be used if not provided
     */
    open suspend fun bind(context: Context,
                          deviceAuthenticator: (type: DeviceBindingAuthenticationType) -> DeviceAuthenticator = deviceAuthenticatorIdentifier) {
        execute(context, deviceAuthenticator(deviceBindingAuthenticationType))
    }


    /**
     * Helper method to execute binding , signing, show biometric prompt.
     *
     * @param context  The Application Context
     * @param listener The Listener to listen for the result
     * @param deviceAuthenticator Interface to find the Authentication Type
     * @param encryptedPreference Persist the values in encrypted shared preference
     */
    @JvmOverloads
    internal suspend fun execute(context: Context,
                                 deviceAuthenticator: DeviceAuthenticator = getDeviceAuthenticator(
                                     deviceBindingAuthenticationType),
                                 encryptedPreference: DeviceRepository = SharedPreferencesDeviceRepository(
                                     context),
                                 deviceId: String = DeviceIdentifier.builder().context(context)
                                     .build().identifier) {


        deviceAuthenticator.initialize(userId, Prompt(title, subtitle, description))

        if (deviceAuthenticator.isSupported(context).not()) {
            handleException(DeviceBindingException(Unsupported()))
            return
        }

        //TODO We may need to delete other keys if we only want to maintain one keys on the device
        //TODO However, we may want to have Application Pin as fallback, so for now, keep multiple keys.
        var keyPair: KeyPair? = null
        try {
            val status: DeviceBindingStatus
            withTimeout(getDuration(timeout)) {
                keyPair = deviceAuthenticator.generateKeys(context)
                status = deviceAuthenticator.authenticate(context)
            }
            when (status) {
                is Success -> {
                    keyPair?.let {
                        val kid = encryptedPreference.persist(userId,
                            userName,
                            it.keyAlias,
                            deviceBindingAuthenticationType)
                        val jws = deviceAuthenticator.sign(it,
                            kid,
                            userId,
                            challenge,
                            getExpiration(timeout))
                        setJws(jws)
                        setDeviceId(deviceId)
                    }
                }
                is DeviceBindingErrorStatus -> {
                    handleException(DeviceBindingException(status))
                }
            }
        } catch (e: Exception) {
            keyPair?.let { encryptedPreference.delete(it.keyAlias) }
            deviceAuthenticator.deleteKeys(context)
            handleException(e)
        }
    }
}


/**
 * convert authentication string received from server to authentication enum
 */
enum class DeviceBindingAuthenticationType constructor(val serializedValue: String?) {
    BIOMETRIC_ONLY("BIOMETRIC_ONLY"), BIOMETRIC_ALLOW_FALLBACK("BIOMETRIC_ALLOW_FALLBACK"), NONE("NONE"), APPLICATION_PIN(
        "APPLICATION_PIN");
}

fun DeviceBindingAuthenticationType.getAuthType(): DeviceAuthenticator {
    return when (this) {
        DeviceBindingAuthenticationType.BIOMETRIC_ONLY -> BiometricOnly()
        DeviceBindingAuthenticationType.APPLICATION_PIN -> ApplicationPinDeviceAuthenticator()
        DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK -> BiometricAndDeviceCredential()
        else -> None()
    }
}

