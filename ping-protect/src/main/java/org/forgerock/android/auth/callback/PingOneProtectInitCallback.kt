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

private val TAG = PingOneProtectInitCallback::class.java.simpleName

/**
 * Callback to collect the device binding information
 */
open class PingOneProtectInitCallback : RootAbstractCallback {
    @Keep
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)

    @Keep
    constructor() : super()

    lateinit var envId: String
        private set

    var pauseBehavioralData: Boolean? = null
        private set

    var consoleLogEnabled: Boolean? = null
        private set

    final override fun setAttribute(
        name: String,
        value: Any,
    ) = when (name) {
        "envId" -> envId = value as String
        "pauseBehavioralData" -> pauseBehavioralData = value as? Boolean
        "consoleLogEnabled" -> consoleLogEnabled = value as? Boolean
        else -> {}
    }

    override fun getType(): String {
        return "PingOneProtectInitCallback"
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
//    open fun init(
//        context: Context,
//        listener: FRListener<Void>,
//    ) {
//        val scope = CoroutineScope(Dispatchers.Default)
//        scope.launch {
//            try {
//                init(context)
//                Listener.onSuccess(listener, null)
//            } catch (e: Exception) {
//                Listener.onException(listener, e)
//            }
//        }
//    }

    open suspend fun init(context: Context) {
        try {
            PingOneProtect().setInitCallback(context, this)
        } catch (e: Exception) {
            Logger.error(TAG, t = e, message = e.message)
            setClientError("ClientErrors")
            throw e
        }
    }
}

class PingOneInitException(message: String) : Exception(message)
