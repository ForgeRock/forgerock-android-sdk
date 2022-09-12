/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity

import org.forgerock.android.auth.*
import org.forgerock.android.auth.devicebind.*
import org.json.JSONObject


/**
 * Callback to collect the device binding information
 */
open class DeviceSigningVerifierCallback: AbstractCallback {

    @JvmOverloads constructor(jsonObject: JSONObject,
                              index: Int): super(jsonObject, index)

    @JvmOverloads constructor(): super()

    /**
     * The optional userId
     */
    private var userId: String? = null

    /**
     * The challenge received from server
     */
    private lateinit var challenge: String
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
        "userId" ->  userId = value as? String
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
     * @param value DeviceBind ErrorType .
     */
    fun setClientError(value: String?) {
        super.setValue(value, 1)
    }

    /**
     * Bind the device.
     *
     * @param context  The Application Context
     * @param listener The Listener to listen for the result
     */
    @JvmOverloads
    fun sign(context: Context,
             listener: FRListener<Void>) {
        execute(context, listener = listener)
    }

    /**
     * Helper method to execute binding , signing, show biometric prompt.
     *
     * @param context  The Application Context
     * @param listener The Listener to listen for the result
     * @param authInterface Interface to find the Authentication Type
     * @param encryptedPreference Persist the values in encrypted shared preference
     */
    protected open fun authenticate(userKey: UserKey,
                                    listener: FRListener<Void>,
                                    authInterface: Authenticator = getAuthenticator(userKey)) {

        if(authInterface.isSupported().not()) {
            handleException(Unsupported(), listener)
            return
        }
        try {
            authInterface.authenticate(timeout ?: 60) { result ->
                if (result is Success) {
                    val jws = authInterface.sign(userKey, challenge)
                    setJws(jws)
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
     * create the interface for the Authentication type(Biometric, Biometric_Fallback, none)
     */
    protected open fun execute(context: Context,
                               userKeyService: UserKeyService = UserDeviceKeyService(context),
                               listener: FRListener<Void>) {

        when(val status = userKeyService.getKeyStatus(userId)) {
            is NoKeysFound -> handleException(UnRegister(), listener)
            is SingleKeyFound -> authenticate(status.key , listener = listener)
            else -> { showKeysFragment(InitProvider.getCurrentActivityAsFragmentActivity(), userKeyService, listener =  listener) }
        }
    }

    protected open fun showKeysFragment(activity: FragmentActivity,
                                        userKeyService: UserKeyService = UserDeviceKeyService(activity),
                                        fragment: DialogFragment =  DeviceBindFragment(userKeyService),
                                        listener: FRListener<Void>) {

        userKeyService.callback = {
            authenticate(it, listener = listener)
        }
        fragment.show(activity.supportFragmentManager, DeviceBindFragment.TAG)
    }

    /**
     * create the interface for the Authentication type(Biometric, Biometric_Fallback, none)
     */
    protected open fun getAuthenticator(key: UserKey): Authenticator {
        return AuthenticatorFactory.getType(key.userId, key.authType, title, subtitle, description)
    }

    /**
     * Handle all the errors for the device binding.
     *
     * @param status  DeviceBindingStatus(timeout,Abort, unsupported)
     * @param listener The Listener to listen for the result
     */
    protected open fun handleException(status: DeviceBindingStatus,
                             listener: FRListener<Void>) {
        setClientError(status.clientError)
        Logger.error(tag, status.message, status.errorCode)
        Listener.onException(
            listener,
            DeviceBindingException(status.message)
        )
    }

}