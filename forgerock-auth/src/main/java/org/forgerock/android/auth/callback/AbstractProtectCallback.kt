/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

import androidx.annotation.Keep
import org.forgerock.android.auth.Node
import org.json.JSONException
import org.json.JSONObject

const val _ACTION = "_action"
const val _TYPE = "_type"
const val CLIENT_ERROR = "clientError"
const val PING_ONE_RISK_EVALUATION_SIGNALS = "pingone_risk_evaluation_signals"
const val PING_ONE_PROTECT = "PingOneProtect"
const val PROTECT_INITIALIZE = "protect_initialize"
const val PROTECT_RISK_EVALUATION = "protect_risk_evaluation"
const val PING_ONE_PROTECT_INITIALIZE_CALLBACK = "PingOneProtectInitializeCallback"
const val PING_ONE_PROTECT_EVALUATION_CALLBACK = "PingOneProtectEvaluationCallback"

/**
 * Abstract Protect Callback that provides the raw content of the Callback, and common methods
 * for sub classes to access.
 */
abstract class AbstractProtectCallback: NodeAware, AbstractCallback {

    /**
     * Indicates if this callback is derived from a [MetadataCallback]
     */
    protected var derivedCallback: Boolean = false

    /**
     * Constructor for [AbstractProtectCallback]. Capable of parsing the [MetadataCallback] used for Protect.
     */
    @Keep
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index) {
        val type = jsonObject.optString("type")
        if (type == "MetadataCallback") {
            derivedCallback = true
            jsonObject.optJSONArray("output")?.let { output ->
                output.getJSONObject(0)?.let { nameValuePair ->
                    if (nameValuePair.optString("name") == "data") {
                        nameValuePair.optJSONObject("value")?.let { value ->
                            value.keys().forEach { attr ->
                                setAttribute(attr, value.get(attr))
                            }
                        }
                    }
                }
            }
        }
    }

    @Keep
    constructor() : super()

    /**
     * The [Node] that associate with this Callback
     */
    private lateinit var node: Node

    override fun setNode(node: Node) {
        this.node = node
    }

    /**
     * Get the [Node] that associate with this Callback
     *
     * @return The [Node] that associate with this Callback
     */
    protected fun getNode(): Node {
        return node
    }

    /**
     * Input the Client Error to the server
     *
     * @param value Protect ErrorType
     * @param index The index of the error
     */
    fun setClientError(value: String, index: Int) {
        if (derivedCallback) {
            setClientErrorInHiddenCallback(value);
        } else {
            super.setValue(value, index)
        }
    }

    /**
     * Set the client error to the [HiddenValueCallback] which associated with the Protect
     * Callback.
     *
     * @param value The Value to set to the [HiddenValueCallback]
     */
    private fun setClientErrorInHiddenCallback(value: String) {
        for (callback in node.callbacks) {
            if (callback is HiddenValueCallback) {
                if (callback.id.contains(CLIENT_ERROR)) {
                    callback.value = value
                }
            }
        }
    }

    companion object {

        /**
         * Check if this callback is [PingOneProtectInitializeCallback] Type
         *
         * @param value The callback raw data json.
         * @return True if this is a [PingOneProtectInitializeCallback] Type, else false
         */
        @JvmStatic
        fun isPingOneProtectInitializeCallback(value: JSONObject): Boolean {
            return try {
                value.has(_TYPE) && value.getString(_TYPE) == PING_ONE_PROTECT && (value.has(
                    _ACTION
                ) && value.getString(_ACTION) == PROTECT_INITIALIZE)
            } catch (e: JSONException) {
                false
            }
        }

        /**
         * Check if this callback is [PingOneProtectEvaluationCallback] Type
         *
         * @param value The callback raw data json.
         * @return True if this is a [PingOneProtectEvaluationCallback] Type, else false
         */
        @JvmStatic
        fun isPingOneProtectEvaluationCallback(value: JSONObject): Boolean {
            return try {
                value.has(_TYPE) && value.getString(_TYPE) == PING_ONE_PROTECT && (value.has(
                    _ACTION
                ) && value.getString(_ACTION) == PROTECT_RISK_EVALUATION)
            } catch (e: JSONException) {
                false
            }
        }

    }

}