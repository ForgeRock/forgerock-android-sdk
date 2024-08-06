/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
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
 * Callback designed for usage with the ReCaptchaEnterpriseNode.
 */
class ReCaptchaEnterpriseCallback : AbstractCallback {
    /**
     * Retrieves the specified site key.
     *
     * @return the site key.
     */
    lateinit var reCaptchaSiteKey: String
        private set

    companion object {
        private val TAG = ReCaptchaEnterpriseCallback::class.java.simpleName
        private const val INVALID_CAPTCHA_TOKEN = "INVALID_CAPTCHA_TOKEN"
        private const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
    }


    @Keep
    @JvmOverloads
    constructor()

    /**
     * Constructor that creates a [ReCaptchaEnterpriseCallback].
     */
    @Keep
    @JvmOverloads
    constructor(raw: JSONObject?, index: Int) : super(raw, index)

    override fun setAttribute(name: String, value: Any) {
        if ("recaptchaSiteKey" == name) {
            this.reCaptchaSiteKey = value as String
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

    /**
     * Set the Payload for the ReCAPTCHA
     *
     * @param value The Payload
     */
    fun setPayload(value: JSONObject) {
        super.setValue(value.toString(), 2)
    }

    override fun getType(): String {
        return "ReCaptchaEnterpriseCallback"
    }

    /**
     * Proceeds with the enterprise reCAPTCHA validation.
     *
     * @param application The application context.
     * @param action The action to be taken (default is "login").
     * @param timeoutInMillis The timeout for the action (default is 10 seconds).
     * @throws Exception for any other errors during validation.
     */
    suspend fun execute(application: Application,
                        action: String = "login",
                        timeoutInMillis: Long = 10000L,
                        provider: RecaptchaClientProvider = object : RecaptchaClientProvider {}) {
        try {
            val recaptchaClient = provider.fetchClient(application, reCaptchaSiteKey)
            val token = provider.execute(
                recaptchaClient,
                RecaptchaAction.custom(action),
                timeoutInMillis
            )
            if (token == null) {
                throw Exception(INVALID_CAPTCHA_TOKEN)
            }
            setValue(token)
        }
        catch (e: Exception) {
            Logger.error(TAG, e.message)
            setClientError(e.message ?: UNKNOWN_ERROR)
            throw e
        }
    }
}

/**
 * Interface for the RecaptchaClientProvider.
 */
interface RecaptchaClientProvider {
    /**
     * Fetches the RecaptchaClient.
     *
     * @param application The application context.
     * @param siteKey The site key.
     * @return The RecaptchaClient or null.
     */

     suspend fun fetchClient(application: Application, siteKey: String): RecaptchaClient {
        return Recaptcha.fetchClient(application, siteKey)
    }

    /**
     * Executes the specified action.
     *
     * @param client The RecaptchaClient.
     * @param action The RecaptchaAction.
     * @param timeoutInMillis The timeout for the action.
     * @return The token or null.
     */
     suspend fun execute(client: RecaptchaClient, action: RecaptchaAction, timeoutInMillis: Long): String? {
        return client.execute(action, timeoutInMillis).getOrNull()
    }
}