/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import org.forgerock.android.auth.Logger.Companion.debug
import org.forgerock.android.auth.callback.Callback
import org.forgerock.android.auth.callback.CallbackFactory
import org.forgerock.android.auth.callback.DerivableCallback
import org.forgerock.android.auth.callback.MetadataCallback
import org.forgerock.android.auth.callback.NodeAware
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import javax.security.auth.callback.UnsupportedCallbackException

/**
 * Interface for an object that listens to changes resulting from a [AuthService].
 */
interface NodeListener<T> : FRListener<T> {
    /**
     * Notify the listener that the [AuthService] has been started and moved to the first node.
     *
     * @param node The first Node
     */
    fun onCallbackReceived(node: Node)

    /**
     * Transform the response from AM Intelligent Tree to Node Object, after the transformation
     * [.onCallbackReceived] will be invoked with the returned [Node].
     *
     * @param authServiceId Unique Auth Service Id
     * @param response      The JSON Response from AM Intelligent Tree
     * @return The Node Object
     * @throws Exception Any error during the transformation
     */
    @Throws(Exception::class)
    fun onCallbackReceived(authServiceId: String,
                           response: JSONObject): Node {
        val callbacks =
            parseCallback(response.getJSONArray("callbacks"))
        val node = Node(response.getString(Node.AUTH_ID),
            response.optString(Node.STAGE, getStage(callbacks)),
            response.optString(Node.HEADER, null),
            response.optString(Node.DESCRIPTION, null),
            authServiceId,
            callbacks)
        callbacks.forEach {
            if (it is NodeAware) {
                it.setNode(node)
            }
        }
        return node
    }

    /**
     * Parse the JSON Array callback response from AM, and transform to [Callback] instances.
     *
     * @param jsonArray The JSON Array callback response from AM
     * @return A List of [Callback] Object
     * @throws Exception Any error during the transformation
     */
    @Throws(Exception::class)
    fun parseCallback(jsonArray: JSONArray): List<Callback> {
        val callbacks: MutableList<Callback> = ArrayList()
        for (i in 0 until jsonArray.length()) {
            val cb = jsonArray.getJSONObject(i)
            val type = cb.getString("type")
            // Return the Callback Class which represent the Callback from AM
            val clazz = CallbackFactory.getInstance().callbacks[type]
                ?: //When Callback is not registered to the SDK
                throw UnsupportedCallbackException(null,
                    "Callback Type Not Supported: " + cb.getString("type"))
            var callback =
                clazz.getConstructor(JSONObject::class.java, Int::class.javaPrimitiveType)
                    .newInstance(cb, i)
            if (callback is DerivableCallback) {
                val derivedClass = (callback as DerivableCallback).derivedCallback
                if (derivedClass != null) {
                    callback = derivedClass.getConstructor(JSONObject::class.java,
                        Int::class.javaPrimitiveType).newInstance(cb, i)
                } else {
                    debug(TAG, "Derive class not found.")
                }
            }
            callbacks.add(callback)
        }
        return callbacks
    }

    /**
     * Workaround stage property for AM version < 7.0.
     * https://github.com/jaredjensen/forgerock-sdk-blog/blob/master/auth_tree_stage.md
     *
     * @param callbacks Callback from Intelligent Tree
     * @return stage or null if not found.
     */
    fun getStage(callbacks: List<Callback>): String? {
        for (callback in callbacks) {
            if (callback.javaClass == MetadataCallback::class.java) {
                try {
                    return (callback as MetadataCallback).value.getString("stage")
                } catch (e: JSONException) {
                    //ignore and continue to find the next metadata callback.
                }
            }
        }
        return null
    }

    companion object {
        @JvmField
        val TAG = NodeListener::class.java.simpleName
    }
}
