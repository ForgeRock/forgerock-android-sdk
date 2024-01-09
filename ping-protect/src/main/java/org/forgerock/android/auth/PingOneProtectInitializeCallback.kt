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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Collections

private val TAG = PingOneProtectInitCallback::class.java.simpleName

/**
 * Callback to initialize the ping one protect
 */
open class PingOneProtectInitCallback : AbstractCallback {
    @Keep
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)

    @Keep
    constructor() : super()

    var envId: String? = null
        private set
    var behavioralDataCollection: Boolean? = null
        private set

    var consoleLogEnabled: Boolean? = null
        private set

    var lazyMetadata: Boolean? = null
        private set

    var customHost: String? = null
        private set
    var deviceAttributesToIgnore: List<String>? = null
        private set

    /**
     * Get the getDeviceAttributes attribute
     *
     * @param array The data source
     */
    private fun getDeviceAttributes(array: JSONArray?): List<String>? {
        val list: MutableList<String> = ArrayList()
        return array?.let {
            for (i in 0 until array.length()) {
                try {
                    list.add(array.getString(i))
                } catch (e: JSONException) {
                    return null
                }
            }
            return Collections.unmodifiableList(list)
        }
    }
    final override fun setAttribute(
        name: String,
        value: Any,
    ) = when (name) {
        "envId" -> envId = value as? String
        "behavioralDataCollection" -> behavioralDataCollection = value as? Boolean
        "consoleLogEnabled" -> consoleLogEnabled = value as? Boolean
        "deviceAttributesToIgnore" -> deviceAttributesToIgnore = getDeviceAttributes(value as? JSONArray)
        "customHost" -> customHost = value as? String
        "lazyMetadata" -> lazyMetadata = value as? Boolean
        else -> {}
    }

    override fun getType(): String {
        return "PingOneProtectInitializeCallback"
    }

    /**
     * Input the Client Error to the server
     * @param value DeviceBind ErrorType .
     */
    fun setClientError(value: String) {
        super.setValue(value, 0)
    }

    /**
     * Collect the behavior. Calling the [start] function.
     *
     * @param context The Application Context
     */
    open suspend fun start(context: Context) {
        try {
            val init =
                PIInitParams(
                    envId = envId,
                    isBehavioralDataCollection = behavioralDataCollection ?: true,
                    isLazyMetadata = lazyMetadata ?: false,
                    isConsoleLogEnabled = consoleLogEnabled ?: false,
                    deviceAttributesToIgnore = deviceAttributesToIgnore,
                    customHost = customHost,
                )
            PIProtect.initSDK(context, init)
            if(behavioralDataCollection == true) {
                PIProtect.resumeBehavioralData()
            } else {
                PIProtect.pauseBehavioralData()
            }
        } catch (e: Exception) {
            Logger.error(TAG, t = e, message = e.message)
            setClientError(e.message ?: "clientError")
            throw e
        }
    }
}
class PingOneProtectInitException(message: String?) : Exception(message)
