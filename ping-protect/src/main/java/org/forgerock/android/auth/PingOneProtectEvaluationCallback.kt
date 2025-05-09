/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import androidx.annotation.Keep
import org.forgerock.android.auth.callback.AbstractProtectCallback
import org.forgerock.android.auth.callback.HiddenValueCallback
import org.forgerock.android.auth.callback.PING_ONE_RISK_EVALUATION_SIGNALS
import org.json.JSONObject

private val TAG = PingOneProtectEvaluationCallback::class.java.simpleName

/**
 * Callback to evaluate Ping One Protect
 */
open class PingOneProtectEvaluationCallback : AbstractProtectCallback {

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
        if (derivedCallback) {
            setSignalsInHiddenCallback(value)
        } else {
            super.setValue(value, 0)
        }
    }

    /**
     * Set the signals to the [HiddenValueCallback] which associated with the callback.
     *
     * @param value The Value to set to the [HiddenValueCallback].
     */
    private fun setSignalsInHiddenCallback(value: String) {
        for (callback in getNode().callbacks) {
            if (callback is HiddenValueCallback) {
                if (callback.id.contains(PING_ONE_RISK_EVALUATION_SIGNALS)) {
                    callback.value = value
                }
            }
        }
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
            setClientError(e.message ?: "clientError", 1)
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