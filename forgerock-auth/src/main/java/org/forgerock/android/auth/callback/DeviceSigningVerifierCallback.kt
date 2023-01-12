/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import android.os.OperationCanceledException
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.Listener
import org.forgerock.android.auth.devicebind.DeviceAuthenticator
import org.forgerock.android.auth.devicebind.DeviceBindFragment
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.*
import org.forgerock.android.auth.devicebind.DeviceBindingException
import org.forgerock.android.auth.devicebind.NoKeysFound
import org.forgerock.android.auth.devicebind.Prompt
import org.forgerock.android.auth.devicebind.SingleKeyFound
import org.forgerock.android.auth.devicebind.Success
import org.forgerock.android.auth.devicebind.UserDeviceKeyService
import org.forgerock.android.auth.devicebind.UserKey
import org.forgerock.android.auth.devicebind.UserKeyService
import org.forgerock.android.auth.devicebind.UserKeys
import org.forgerock.android.auth.devicebind.initialize
import org.json.JSONObject


/**
 * Callback to collect the device signing information
 */
open class DeviceSigningVerifierCallback : AbstractCallback, Binding {

    @JvmOverloads
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)

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
    override fun setClientError(value: String?) {
        super.setValue(value, 1)
    }

    /**
     * Sign the device.
     *
     * @param context  The Application Context
     * @param listener The Listener to listen for the result
     */
    open suspend fun sign(context: Context) {
        execute(context)
    }


    /**
     * Bind the device.
     *
     * @param context  The Application Context
     * @param listener The Listener to listen for the result
     */
    open fun sign(context: Context, listener: FRListener<Void>) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                sign(context)
                Listener.onSuccess(listener, null)
            } catch (e: Exception) {
                Listener.onException(listener, e)
            }
        }
    }

    /**
     * Helper method to execute signing, show biometric prompt.
     *
     * @param userKey User Information
     * @param listener The Listener to listen for the result
     * @param deviceAuthenticator Interface to find the Authentication Type
     */
    @JvmOverloads
    protected open suspend fun authenticate(context: Context,
                                            userKey: UserKey,
                                            deviceAuthenticator: DeviceAuthenticator = getDeviceBindAuthenticator(
                                                userKey)) {

        deviceAuthenticator.initialize(userKey.userId, Prompt(title, subtitle, description))

        if (deviceAuthenticator.isSupported(context).not()) {
            handleException(DeviceBindingException(Unsupported()))
        }
        when (val status = deviceAuthenticator.authenticate(context)) {
            is Success -> {
                val jws = deviceAuthenticator.sign(userKey,
                    status.privateKey,
                    challenge,
                    getExpiration(timeout))
                setJws(jws)
            }
            is DeviceBindingErrorStatus -> {
                // All the biometric exception is handled here , it could be Abort or timeout
                handleException(DeviceBindingException(status))
            }
        }

    }

    /**
     * Create the interface for the Authentication type(Biometric, Biometric_Fallback, none)
     * @param context application or activity context
     * @param userKeyService service to sort and fetch the keys stored in the device
     * @param listener The Listener to listen for the result
     */
    @JvmOverloads
    internal suspend fun execute(context: Context,
                                 userKeyService: UserKeyService = UserDeviceKeyService(context)) {

        try {
            withTimeout(getDuration(timeout)) {
                when (val status = userKeyService.getKeyStatus(userId)) {
                    is NoKeysFound -> handleException(DeviceBindingException(UnRegister()))
                    is SingleKeyFound -> authenticate(context, status.key)
                    else -> {
                        val userKey = getUserKey(userKeyService = userKeyService)
                        authenticate(context, userKey)
                    }
                }
            }
        } catch (e: Exception) {
            // This Exception happens only when there is Signing or keypair failed.
            handleException(e)
        }
    }

    /**
     * Display fragment to select a user key from the list
     * @param activity activity to be used to display the Fragment
     * @param userKeyService service to sort and fetch the keys stored in the device
     * @param listener The Listener to listen for the result
     */
    protected open suspend fun getUserKey(activity: FragmentActivity = InitProvider.getCurrentActivityAsFragmentActivity(),
                                          userKeyService: UserKeyService): UserKey =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val existing =
                    activity.supportFragmentManager.findFragmentByTag(DeviceBindFragment.TAG) as? DeviceBindFragment
                if (existing != null) {
                    existing.continuation = continuation
                } else {
                    DeviceBindFragment.newInstance(UserKeys(userKeyService.userKeys), continuation)
                        .apply {
                            this.show(activity.supportFragmentManager, DeviceBindFragment.TAG)
                        }
                }
            }
        }

    /**
     * Create the interface for the Authentication type(Biometric, Biometric_Fallback, none)
     * @param userKey selected UserKey from the device
     */
    protected open fun getDeviceBindAuthenticator(userKey: UserKey): DeviceAuthenticator {
        return getDeviceAuthenticator(userKey.authType)
    }



}