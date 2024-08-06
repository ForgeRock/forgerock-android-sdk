/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.app.Application
import androidx.annotation.Keep
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaAction
import com.google.android.recaptcha.RecaptchaClient
import org.forgerock.android.auth.Logger
import org.json.JSONObject

/**
 * Callback designed for usage with the ReCaptchaNode.
 */
class CaptchaEnterpriseCallback : AbstractCallback {
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
     * Constructor that creates a [CaptchaEnterpriseCallback].
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
    fun setValue(value: String) {
        super.setValue(value, 0)
    }

    /**
     * Set the Action for the ReCAPTCHA
     *
     * @param value The Action
     */
    fun setAction(value: String) {
        super.setValue(value, 1)
    }

    /**
     * Input the Client Error to the server
     * @param value Error String.
     */
    fun setClientError(value: String) {
        super.setValue(value, 2)
    }


    suspend fun proceedEnterprise(application: Application,
                                  action: String? = null,
                                  timeoutInMillis: Long = 10000L) {
        val recaptchaClient: RecaptchaClient? = Recaptcha.getClient(application, reCaptchaSiteKey).getOrNull()
        recaptchaClient?.let {
            it.execute(RecaptchaAction.custom(action ?: "login"), timeoutInMillis)
                .onSuccess { token ->
                //    setClientError("ReCaptcha Failed")
                    setValue(token)
                    action?.let { action -> setAction(action) }
                }
                .onFailure { exception ->
                    Logger.error("ReCaptcha Failed", exception.message)
                    setClientError(exception.message ?: "ReCaptcha Failed")
                    throw exception
                }
        } ?: run {
            setClientError("ReCaptcha Initialization Failed")
            throw Exception("Initialization Failed")
        }
    }

    override fun getType(): String {
        return "ReCaptchaEnterpriseCallback"
    }
}
