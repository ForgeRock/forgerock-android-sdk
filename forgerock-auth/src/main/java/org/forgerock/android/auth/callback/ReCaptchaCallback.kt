/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.app.Application
import android.content.Context
import androidx.annotation.Keep
import com.google.android.gms.safetynet.SafetyNet
import com.google.android.gms.safetynet.SafetyNetApi.RecaptchaTokenResponse
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaAction
import com.google.android.recaptcha.RecaptchaClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Listener
import org.json.JSONObject
import kotlin.coroutines.resumeWithException

/**
 * Callback designed for usage with the ReCaptchaNode.
 */
class ReCaptchaCallback : AbstractCallback {
    /**
     * Retrieves the specified site key.
     *
     * @return the site key.
     */
    lateinit var reCaptchaSiteKey: String

    lateinit var captchaType: String

    @Keep
    constructor()

    /**
     * Constructor that creates a [ReCaptchaCallback].
     */
    @Keep
    constructor(raw: JSONObject?, index: Int) : super(raw, index)

    override fun setAttribute(name: String, value: Any) {
        if ("recaptchaSiteKey" == name) {
            this.reCaptchaSiteKey = value as String
        }
        else if ("type" == name) {
            this.captchaType = value as String
        }
    }

    /**
     * Set the Value for the ReCAPTCHA
     *
     * @param token The Token received from the captcha server
     */
    fun setValue(token: String) {
        super.setValue(token)
    }


    suspend fun proceedEnterprise(context: Application) {
        val recaptchaClient: RecaptchaClient? = Recaptcha.getClient(context, reCaptchaSiteKey).getOrNull()
        recaptchaClient?.execute(RecaptchaAction.custom("LoginJourney"))?.let {
            if(it.isSuccess) {
                it.getOrNull()?.let {token ->
                    setValue(token)
                } ?: throw Exception("Empty token")
            } else {
                throw Exception("ReCaptcha Failed")
            }
        } ?: throw Exception("Initialization Failed")

    }

    suspend fun proceed(context: Context) {
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                SafetyNet.getClient(context).verifyWithRecaptcha(reCaptchaSiteKey)
                    .addOnSuccessListener { response: RecaptchaTokenResponse ->
                        val userResponseToken = response.tokenResult
                        if (userResponseToken?.isNotEmpty() == true) {
                            setValue(userResponseToken)
                            continuation.resumeWith(Result.success(userResponseToken))
                        }
                    }
                    .addOnFailureListener { e: Exception? ->
                        continuation.resumeWithException(e ?: Exception("ReCaptcha Failed"))
                    }
            }
        }
    }

    /**
     * Proceed to trigger the ReCAPTCHA
     *
     * @param context  The Application Context
     * @param listener Listener to lister for ReCAPTCHA result event.
     */
    fun proceed(context: Context, listener: FRListener<Void?>?, application: Application? = null) {
        try {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                application?.let {
                    proceedEnterprise(it)
                } ?: run {
                    Listener.onException(listener, Exception("Application is null"))
                }
                Listener.onSuccess(listener, null)
            }
        }
        catch (e: Exception) {
            Listener.onException(listener, e)
        }
    }

    override fun getType(): String {
        return "ReCaptchaCallback"
    }
}
