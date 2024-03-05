/*
 * Copyright (c) 2022 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Listener
import org.forgerock.android.auth.devicebind.DefaultUserKeySelector
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.ClientNotRegistered
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.Unsupported
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.InvalidCustomClaims
import org.forgerock.android.auth.devicebind.DeviceBindingException
import org.forgerock.android.auth.devicebind.NoKeysFound
import org.forgerock.android.auth.devicebind.Prompt
import org.forgerock.android.auth.devicebind.SingleKeyFound
import org.forgerock.android.auth.devicebind.Success
import org.forgerock.android.auth.devicebind.UserDeviceKeyService
import org.forgerock.android.auth.devicebind.UserKey
import org.forgerock.android.auth.devicebind.UserKeySelector
import org.forgerock.android.auth.devicebind.UserKeyService
import org.forgerock.android.auth.devicebind.UserKeys
import org.forgerock.android.auth.devicebind.initialize
import org.json.JSONObject


/**
 * Callback to collect the device signing information
 */
open class DeviceSigningVerifierCallback : AbstractCallback, Binding {

    @Keep
    @JvmOverloads
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)

    @Keep
    @JvmOverloads
    constructor() : super()

    /**
     * The optional userId
     */
    var userId: String? = null
        private set

    /**
     * The challenge received from server
     */
    lateinit var challenge: String
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

    override fun setAttribute(name: String, value: Any) = when (name) {
        "userId" -> userId = value as? String
        "challenge" -> challenge = value as String
        "title" -> title = value as? String ?: ""
        "subtitle" -> subtitle = value as? String ?: ""
        "description" -> description = value as? String ?: ""
        "timeout" -> timeout = value as? Int
        else -> {}
    }

    override fun getType(): String {
        return "DeviceSigningVerifierCallback"
    }

    /**
     * Input the JWS key to the server
     * @param value The JWS value.
     */
    fun setJws(value: String?) {
        super.setValue(value, 0)
    }


    /**
     * Input the Client Error to the server
     * @param value DeviceSign ErrorType .
     */
    override fun setClientError(clientError: String?) {
        super.setValue(clientError, 1)
    }

    /**
     * Sign the challenge with bounded device keys.
     *
     * @param context  The Application Context
     * @param customClaims A map of custom claims to be added to the jws payload
     * @param prompt The Prompt to modify the title, subtitle, description
     * @param userKeySelector Collect user key, if not provided [DefaultUserKeySelector] will be used
     * @param deviceAuthenticator A function to return a [DeviceAuthenticator], [deviceAuthenticatorIdentifier] will be used if not provided
     */
    open suspend fun sign(context: Context,
                          customClaims: Map<String, Any> = emptyMap(),
                          prompt: Prompt? = null,
                          userKeySelector: UserKeySelector = DefaultUserKeySelector(),
                          deviceAuthenticator: (type: DeviceBindingAuthenticationType) -> DeviceAuthenticator = deviceAuthenticatorIdentifier) {
        execute(context,
            userKeySelector = userKeySelector,
            deviceAuthenticator = deviceAuthenticator,
            customClaims = customClaims, prompt = prompt)
    }


    /**
     * Sign the challenge with bounded device keys.
     *
     * @param context  The Application Context
     * @param customClaims A map of custom claims to be added to the jws payload
     * @param userKeySelector Collect user key, if not provided [DefaultUserKeySelector] will be used
     * @param deviceAuthenticator A function to return a [DeviceAuthenticator], [deviceAuthenticatorIdentifier] will be used if not provided
     * @param listener The Listener to listen for the result
     * @param prompt The Prompt to modify the title, subtitle, description
     */
    @JvmOverloads
    open fun sign(context: Context,
                  customClaims: Map<String, Any> = emptyMap(),
                  userKeySelector: UserKeySelector = DefaultUserKeySelector(),
                  deviceAuthenticator: (type: DeviceBindingAuthenticationType) -> DeviceAuthenticator = deviceAuthenticatorIdentifier,
                  listener: FRListener<Void?>,
                  prompt: Prompt? = null) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                sign(context, customClaims, prompt, userKeySelector, deviceAuthenticator)
                Listener.onSuccess(listener, null)
            } catch (e: Exception) {
                Listener.onException(listener, e)
            }
        }
    }

    /**
     * Helper method to execute signing, show biometric prompt.
     *
     * @param context Application Context
     * @param userKey User Information
     * @param deviceAuthenticator A function to return a [DeviceAuthenticator], [getDeviceAuthenticator] will be used if not provided
     * @param customClaims A map of custom claims to be added to the jws payload
     * @param prompt The Prompt to modify the title, subtitle, description
     */
    protected open suspend fun authenticate(context: Context,
                                            userKey: UserKey,
                                            deviceAuthenticator: DeviceAuthenticator,
                                            customClaims: Map<String, Any> = emptyMap(),
                                            prompt: Prompt? = null) {

        deviceAuthenticator.initialize(userKey.userId, prompt?: Prompt(title, subtitle, description))

        if (deviceAuthenticator.isSupported(context).not()) {
            handleException(DeviceBindingException(Unsupported()))
        }

        if (deviceAuthenticator.validateCustomClaims(customClaims).not()) {
            handleException(DeviceBindingException(InvalidCustomClaims()))
            return
        }

        when (val status = deviceAuthenticator.authenticate(context)) {
            is Success -> {
                val jws = deviceAuthenticator.sign(context,
                    userKey,
                    status.privateKey,
                    status.signature,
                    challenge,
                    getExpiration(timeout),
                    customClaims)
                setJws(jws)
            }
            is DeviceBindingErrorStatus -> {
                // All the biometric exception is handled here , it could be Abort or timeout
                handleException(DeviceBindingException(status))
            }
        }

    }

    internal suspend fun execute(context: Context,
                                 userKeyService: UserKeyService = UserDeviceKeyService(context),
                                 userKeySelector: UserKeySelector = DefaultUserKeySelector(),
                                 deviceAuthenticator: (type: DeviceBindingAuthenticationType) -> DeviceAuthenticator,
                                 customClaims: Map<String, Any> = emptyMap(),
                                 prompt: Prompt? = null) {

        try {
            withTimeout(getDuration(timeout)) {
                when (val status = userKeyService.getKeyStatus(userId)) {
                    is NoKeysFound -> handleException(DeviceBindingException(ClientNotRegistered()))
                    is SingleKeyFound -> authenticate(context,
                        status.key,
                        deviceAuthenticator(status.key.authType),
                        customClaims,
                        prompt)
                    else -> {
                        val userKey =
                            userKeySelector.selectUserKey(UserKeys(userKeyService.getAll()))
                        authenticate(context, userKey, deviceAuthenticator(userKey.authType), customClaims)
                    }
                }
            }
        } catch (e: Exception) {
            // This Exception happens only when there is Signing or keypair failed.
            handleException(e)
        }
    }

}