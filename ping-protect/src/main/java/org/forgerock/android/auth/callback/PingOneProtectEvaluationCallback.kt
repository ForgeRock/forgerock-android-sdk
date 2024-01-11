/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import androidx.annotation.Keep
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.PingOneProtect
import org.forgerock.android.auth.RootAbstractCallback
import org.json.JSONObject

private val TAG = PingOneProtectEvaluationCallback::class.java.simpleName

/**
 * Callback to collect the device binding information
 */
open class PingOneProtectEvaluationCallback : RootAbstractCallback {

    @Keep
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)

    @Keep
    constructor() : super()

    lateinit var envId: String
        private set


    var pauseBehavioralData: Boolean? = null
        private set

    final override fun setAttribute(name: String, value: Any) = when (name) {
        "envId" -> envId = value as String
        "pauseBehavioralData" -> pauseBehavioralData = value as Boolean
        else -> {}
    }

    override fun getType(): String {
        return "PingOneProtectEvaluationCallback"
    }

    /**
     * Input the Token to the server
     * @param value The JWS value.
     */
    fun setSignals(value: String) {
        super.setValue(value, 0)
    }

    /**
     * Input the Client Error to the server
     * @param value DeviceBind ErrorType .
     */
    fun setClientError(value: String) {
        super.setValue(value, 1)
    }

//    /**
//     * Request for Integrity Token from Google SDK
//     *
//     * @param context  The Application Context
//     * @param listener The Listener to listen for the result
//     */
//    open fun getSignals(context: Context,
//                        listener: FRListener<Void>) {
//        val scope = CoroutineScope(Dispatchers.Default)
//        scope.launch {
//            try {
//                Listener.onSuccess(listener, null)
//            } catch (e: Exception) {
//                Listener.onException(listener, e)
//            }
//        }
//    }

    open suspend fun getSignals(context: Context) {
        try {
            val result = PingOneProtect().getData(this)
            setSignals(result)
        } catch (e: Exception) {
            Logger.error(TAG, t = e, message = e.message)
            setClientError("ClientErrors")
            throw e
        }
    }
}

class PingOneProtectException(message: String) : Exception(message) {
}