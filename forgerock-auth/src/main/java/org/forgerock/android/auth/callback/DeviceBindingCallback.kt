/*
 * Copyright (c) 2022 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import android.util.Base64
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.forgerock.android.auth.CryptoKey
import org.forgerock.android.auth.DeviceIdentifier
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Listener
import org.forgerock.android.auth.devicebind.ApplicationPinDeviceAuthenticator
import org.forgerock.android.auth.devicebind.BiometricAndDeviceCredential
import org.forgerock.android.auth.devicebind.BiometricAuthenticator
import org.forgerock.android.auth.devicebind.BiometricOnly
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.*
import org.forgerock.android.auth.devicebind.DeviceBindingException
import org.forgerock.android.auth.devicebind.DeviceBindingRepository
import org.forgerock.android.auth.devicebind.DeviceBindingStatus
import org.forgerock.android.auth.devicebind.KeyPair
import org.forgerock.android.auth.devicebind.LocalDeviceBindingRepository
import org.forgerock.android.auth.devicebind.None
import org.forgerock.android.auth.devicebind.Prompt
import org.forgerock.android.auth.devicebind.Success
import org.forgerock.android.auth.devicebind.UserKey
import org.forgerock.android.auth.devicebind.initialize
import org.json.JSONObject
import java.util.*

/**
 * Callback to collect the device binding information
 */
open class DeviceBindingCallback : AbstractCallback, Binding {

    @Keep
    @JvmOverloads
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)

    @Keep
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

    /**
     * Enable Attestation
     */
    lateinit var attestation: Attestation
        private set

    init {
        //If attestation is not provided, default to NONE
        if (!::attestation.isInitialized) {
            attestation = Attestation.None
        }
    }

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
        "attestation" -> attestation = Attestation
            .fromBoolean(value as Boolean, Base64.decode(challenge, Base64.NO_WRAP))
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
     * Bind the device. Calling the [bind] function, the existing bounded keys will be removed.
     * If don't want to replace or remove existing keys, please use [FRUserKeys] to check existing
     * keys before calling this method
     *
     * @param context  The Application Context
     * @param deviceAuthenticator A function to return a [DeviceAuthenticator], [deviceAuthenticatorIdentifier] will be used if not provided
     * @param listener The Listener to listen for the result
     * @param prompt The Prompt to modify the title, subtitle, description
     */
    @JvmOverloads
    open fun bind(context: Context,
                  deviceAuthenticator: (type: DeviceBindingAuthenticationType) -> DeviceAuthenticator = deviceAuthenticatorIdentifier,
                  listener: FRListener<Void?>,
                  prompt: Prompt? = null) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                bind(context,  prompt = prompt, deviceAuthenticator = deviceAuthenticator)
                Listener.onSuccess(listener, null)
            } catch (e: Exception) {
                Listener.onException(listener, e)
            }
        }
    }

    /**
     * Bind the device. Calling the [bind] function, the existing bounded keys will be removed.
     * If don't want to replace or remove existing keys, please use [FRUserKeys] to check existing
     * keys before calling this method
     *
     * @param context  The Application Context
     * @param prompt The Prompt to modify the title, subtitle, description
     * @param deviceAuthenticator A function to return a [DeviceAuthenticator], [deviceAuthenticatorIdentifier] will be used if not provided
     */
    open suspend fun bind(context: Context,
                          prompt: Prompt? = null,
                          deviceAuthenticator: (type: DeviceBindingAuthenticationType) -> DeviceAuthenticator = deviceAuthenticatorIdentifier) {
        execute(context, deviceAuthenticator(deviceBindingAuthenticationType), prompt = prompt)
    }


    /**
     * Helper method to execute binding , signing, show biometric prompt.
     *
     * @param context  The Application Context
     * @param deviceAuthenticator Interface to find the Authentication Type
     * @param deviceBindingRepository Persist the values in encrypted shared preference
     * @param deviceId Generated Device Identifier
     * @param prompt The Prompt to modify the title, subtitle, description
     */
    internal suspend fun execute(context: Context,
                                 deviceAuthenticator: DeviceAuthenticator = getDeviceAuthenticator(
                                     deviceBindingAuthenticationType),
                                 deviceBindingRepository: DeviceBindingRepository = LocalDeviceBindingRepository(
                                     context),
                                 deviceId: String = DeviceIdentifier.builder().context(context)
                                     .build().identifier,
                                 prompt: Prompt? = null) {

        deviceAuthenticator.initialize(userId, prompt ?: Prompt(title, subtitle, description))

        if (deviceAuthenticator.isSupported(context, attestation).not()) {
            handleException(DeviceBindingException(Unsupported()))
            return
        }

        var keyPair: KeyPair?
        var userKey: UserKey? = null
        try {
            val status: DeviceBindingStatus
            withTimeout(getDuration(timeout)) {
                clearKeys(context, deviceAuthenticator)
                keyPair =
                    deviceAuthenticator.generateKeys(context, attestation)
                status = deviceAuthenticator.authenticate(context)
            }
            when (status) {
                is Success -> {
                    keyPair?.let { kp ->
                        userKey = UserKey(kp.keyAlias,
                            userId, userName, kid = UUID.randomUUID().toString(),
                            deviceBindingAuthenticationType
                        )
                        userKey?.let {
                            deviceBindingRepository.persist(it)
                            val jws = deviceAuthenticator.sign(context,
                                kp,
                                status.signature,
                                it.kid,
                                userId,
                                challenge,
                                getExpiration(timeout),
                                attestation)
                            setJws(jws)
                            setDeviceId(deviceId)
                        }
                    }
                }
                is DeviceBindingErrorStatus -> {
                    throw DeviceBindingException(status)
                }
            }
        } catch (e: Exception) {
            userKey?.let { deviceBindingRepository.delete(it) }
            deviceAuthenticator.deleteKeys(context)
            handleException(e)
        }
    }

    /**
     * For now we don't support multiple keys, so before we create new keys,
     * we clean existing keys.
     */
    private fun clearKeys(context: Context, deviceAuthenticator: DeviceAuthenticator) {
        when (deviceAuthenticator) {
            is ApplicationPinDeviceAuthenticator -> {
                //Delete Keys from keystore
                getCryptoKey().deleteKeys()
            }
            is BiometricAuthenticator, is None -> {
                ApplicationPinDeviceAuthenticator().initialize(userId).deleteKeys(context)
            }
        }
        deviceAuthenticator.deleteKeys(context)
    }

    open fun getCryptoKey() = CryptoKey(userId)
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

