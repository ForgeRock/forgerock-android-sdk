/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

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
abstract class AbstractProtectCallback(raw: JSONObject, index: Int) : NodeAware, AbstractCallback(raw, index) {

    /**
     * Constructor for [AbstractProtectCallback]. Capable of parsing the [MetadataCallback] used for Protect.
     */
    init {
        val type = raw.optString("type")
        if (type == "MetadataCallback") {
            val output = raw.optJSONArray("output")
            if (output != null) {
                val nameValuePair = output.getJSONObject(0)
                val name = nameValuePair.optString("name")
                if (nameValuePair != null && name == "data") {
                    val value = nameValuePair.getJSONObject("value")
                    for (attr in value.keys()) {
                        setAttribute(attr, value.get(attr))
                    }
                }
            }
        }
    }

    /**
     * The [Node] that associate with this Callback
     */
    private lateinit var node: Node

    override fun setNode(node: Node) {
        this.node = node
    }

    /**
     * Set the client error to the [HiddenValueCallback] which associated with the Protect
     * Callback.
     *
     * @param value The Value to set to the [HiddenValueCallback]
     */
    fun setClientErrorInHiddenCallback(value: String) {
        for (callback in node.callbacks) {
            if (callback is HiddenValueCallback) {
                if (callback.id.contains(CLIENT_ERROR)) {
                    callback.value = value
                }
            }
        }
    }

    /**
     * Set the signals to the [HiddenValueCallback] which associated with the callback.
     *
     * @param value The Value to set to the [HiddenValueCallback].
     */
    fun setSignalsInHiddenCallback(value: String) {
        for (callback in node.callbacks) {
            if (callback is HiddenValueCallback) {
                if (callback.id.contains(PING_ONE_RISK_EVALUATION_SIGNALS)) {
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