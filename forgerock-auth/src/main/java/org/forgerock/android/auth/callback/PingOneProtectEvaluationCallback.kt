/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import androidx.annotation.Keep
import com.pingidentity.signalssdk.sdk.GetDataCallback
import com.pingidentity.signalssdk.sdk.PingOneSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Listener
import org.forgerock.android.auth.Logger
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val TAG = PingOneProtectEvaluationCallback::class.java.simpleName

/**
 * Callback to collect the device binding information
 */
open class PingOneProtectEvaluationCallback : AbstractCallback {

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

    /**
     * Request for Integrity Token from Google SDK
     *
     * @param context  The Application Context
     * @param listener The Listener to listen for the result
     */
    open fun getSignals(context: Context,
                        listener: FRListener<Void>) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                Listener.onSuccess(listener, null)
            } catch (e: Exception) {
                Listener.onException(listener, e)
            }
        }
    }

    open suspend fun getSignals(context: Context) {

        try {
            return suspendCancellableCoroutine {
                PingOneSignals.getData(object : GetDataCallback {
                    override fun onSuccess(result: String) {
                        setSignals(result)
                        pauseBehavioralData?.let { it ->
                            if (it) {
                                PingOneSignals.pauseBehavioralData()
                            }
                        }
                        it.resume(Unit)
                    }

                    override fun onFailure(result: String) {
                        it.resumeWithException(PingOneProtectException(result))
                    }
                })
            }
        } catch (e: Exception) {
            Logger.error(TAG, t = e, message = e.message)
            setClientError("ClientErrors")
            throw e
        }
    }
}

class PingOneProtectException(message: String) : Exception(message) {
}