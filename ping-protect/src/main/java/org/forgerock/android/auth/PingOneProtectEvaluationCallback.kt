/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import androidx.annotation.Keep
import org.forgerock.android.auth.callback.AbstractCallback
import org.json.JSONObject

private val TAG = PingOneProtectEvaluationCallback::class.java.simpleName

/**
 * Callback to evaluate Ping One Protect
 */
open class PingOneProtectEvaluationCallback : AbstractCallback {

    @Keep
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)

    @Keep
    constructor() : super()
    /**
     * The pauseBehavioralData received from server
     */
    var pauseBehavioralData: Boolean? = null
        private set

    final override fun setAttribute(name: String, value: Any) = when (name) {
        "pauseBehavioralData" -> pauseBehavioralData = value as? Boolean
        else -> {}
    }

    override fun getType(): String {
        return "PingOneProtectEvaluationCallback"
    }

    /**
     * Input the Signal to the server
     * @param value The JWS value.
     */
    fun setSignals(value: String) {
        super.setValue(value, 0)
    }

    /**
     * Input the Client Error to the server
     * @param value Protect ErrorType .
     */
    fun setClientError(value: String) {
        super.setValue(value, 1)
    }

    /**
     * Get the signal by Calling the [getData] function.
     *
     * @param context The Application Context
     */
    open suspend fun getData(context: Context) {
        try {
            val result = PIProtect.getData()
            if (pauseBehavioralData == true) {
                PIProtect.pauseBehavioralData()
            }
            setSignals(result)
        }
        catch (e: Exception) {
            Logger.error(TAG, t = e, message = e.message)
            setClientError(e.message ?: "clientError")
            throw e
        }
    }
}

/**
 * Exception to capture PingOneProtect Signal.
 *
 * @param message The Message
 */
class PingOneProtectEvaluationException(message: String?) : Exception(message)