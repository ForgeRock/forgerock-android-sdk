/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import androidx.biometric.BiometricPrompt
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Listener
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.biometric.BiometricAuthCompletionHandler
import org.forgerock.android.auth.devicebind.*
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

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
        "authenticationType" -> {
            deviceBindingAuthenticationType = (value as? String)?.let {
                DeviceBindingAuthenticationType.valueOf(it)
            } ?: DeviceBindingAuthenticationType.NONE
        }
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

    fun setClientError(value: String?) {
        super.setValue(value, 2)
    }

    fun bind(context: Context,
             listener: FRListener<Void>) {
        execute(context, listener)
    }

    internal fun execute(context: Context,
                         listener: FRListener<Void>,
                         keyAware: KeyAwareInterface = KeyAware(userId),
                         biometric: BiometricInterface? = BiometricUtil(title, subtitle, description, deviceBindAuthenticationType = deviceBindingAuthenticationType),
                         encryptedPreference: PreferenceInterface = PreferenceUtil(context)) {

        val keypair: KeyPair
        // If the authentication type is none or the biometric supported then create keypair. if not throw exception
        if(deviceBindingAuthenticationType == DeviceBindingAuthenticationType.NONE || isSupported(biometric)) {
            keypair = createKeyPair(context, keyAware) ?: return
        } else {
            setClientError("Unsupported")
            Logger.error(tag, "keypair creation failed and returning back ")
            Listener.onException(
                listener,
                DeviceKeyPairCreationException("Please verify the biometric or credential settings, we are unable to create key pair")
            )
            return
        }

        if(deviceBindingAuthenticationType == DeviceBindingAuthenticationType.NONE) {
            sign(context, listener, keypair, keyAware, encryptedPreference)
            return
        }
        val startTime = Date().time
        val biometricListener = object: BiometricAuthCompletionHandler {
            override fun onSuccess(result: BiometricPrompt.AuthenticationResult?) {
                val endTime =  TimeUnit.MILLISECONDS.toSeconds(Date().time - startTime)
                if(endTime > (this@DeviceBindingCallback.timeout?.toLong() ?: 60)) {
                    setClientError("Timeout")
                    Listener.onException(
                        listener,
                        DeviceBindBiometricException("Biometric Timeout Exception"))
                } else {
                    sign(context, listener, keypair, keyAware, encryptedPreference)
                }
            }

            override fun onError(errorCode: Int, errorMessage: String?) {
                setClientError("Abort")
                Logger.error(tag, errorMessage)
                Listener.onException(
                    listener,
                    DeviceBindBiometricException("$errorCode: + $errorMessage"))
            }
        }
        biometric?.setListener(biometricListener)
        biometric?.authenticate()

    }

    @JvmOverloads
    open fun createKeyPair(context: Context, keyAware: KeyAwareInterface): KeyPair? {
        return keyAware.generateKeys(context, challenge, deviceBindingAuthenticationType)
    }

    @JvmOverloads
    open fun isSupported(biometricUtil: BiometricInterface?): Boolean {
        return biometricUtil?.isSupported() == true
    }

    @JvmOverloads
    open fun sign(context: Context,
                  listener: FRListener<Void>,
                  keyPair: KeyPair,
                  keyInterface: KeyAwareInterface,
                  encryptedPreference: PreferenceInterface) {
        try {
            val kid = encryptedPreference.persist(userId, keyPair.keyAlias, deviceBindingAuthenticationType)
            val jws = keyInterface.sign(keyPair, kid, challenge)
            setJws(jws)
            Listener.onSuccess(listener, null)
        }
        catch (e: Exception) {
            setClientError("Abort")
            Logger.error(tag, "DeviceBindSigningException - " + e.message)
            Listener.onException(
                listener,
                DeviceBindSigningException("DeviceBindSigningException" + e.message)
            )
        }
    }
}

enum class DeviceBindingAuthenticationType constructor(val serializedValue: String?) {
    BIOMETRIC_ONLY("BIOMETRIC_ONLY"),
    BIOMETRIC_ALLOW_FALLBACK("BIOMETRIC_ALLOW_FALLBACK"),
    NONE("NONE");
}