/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.annotation.TargetApi
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Listener
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.webauthn.WebAuthn
import org.forgerock.android.auth.webauthn.WebAuthnAuthentication
import org.forgerock.android.auth.webauthn.WebAuthnKeySelector
import org.json.JSONException
import org.json.JSONObject

/**
 * A callback that handle WebAuthnAuthentication Node.
 */
@TargetApi(24)
open class WebAuthnAuthenticationCallback : MetadataCallback, WebAuthnCallback {

    @JvmOverloads
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)

    @JvmOverloads
    constructor() : super()

    override fun getType(): String {
        return "WebAuthnAuthenticationCallback"
    }

    @Throws(Exception::class)
    open fun getWebAuthnAuthentication() = WebAuthnAuthentication(value)

    /**
     * Perform WebAuthn Authentication
     *
     * @param context   The Application Context
     * @param node      The Node returned from AM
     * @param selector  Optional - The selector to select which credential key to use. Apply to Username-less only.
     * @param listener  Listener to listen for result
     */
    fun authenticate(context: Context, node: Node,
                     selector: WebAuthnKeySelector = WebAuthnKeySelector.DEFAULT,
                     listener: FRListener<Void>) {

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                authenticate(context, node, selector)
                Listener.onSuccess(listener, null)
            } catch (e: Exception) {
                Listener.onException(listener, e)
            }
        }
    }

    /**
     * Perform WebAuthn Authentication
     *
     * @param context   The Application Context
     * @param node      The Node returned from AM
     * @param selector  The selector to select which credential key to use. Apply to Username-less only.
     */
    suspend fun authenticate(context: Context,
                             node: Node,
                             selector: WebAuthnKeySelector = WebAuthnKeySelector.DEFAULT) {
        try {
            val result = getWebAuthnAuthentication().authenticate(context, selector)
            setHiddenCallbackValue(node, result);
        } catch (e: Exception) {
            setErrorRethrow(node, e)
        }
    }

    companion object {
        /**
         * Check if this callback is [WebAuthnAuthenticationCallback] Type
         *
         * @param value The callback raw data json.
         * @return True if this is a [WebAuthnAuthenticationCallback] Type, else false
         */
        @JvmStatic
        fun instanceOf(value: JSONObject): Boolean {
            //_action is provided AM version >= AM 7.1
            if (value.has(WebAuthn._ACTION)) {
                try {
                    if (value.getString(WebAuthn._ACTION) == WebAuthn.WEBAUTHN_AUTHENTICATION) {
                        return true
                    }
                } catch (e: JSONException) {
                    //Should not happened
                    return false
                }
            }
            return try {
                value.has(WebAuthn._TYPE) && value.getString(WebAuthn._TYPE) == WebAuthn.WEB_AUTHN &&
                        !value.has(WebAuthn.PUB_KEY_CRED_PARAMS) && !value.has(WebAuthn._PUB_KEY_CRED_PARAMS)
            } catch (e: JSONException) {
                false
            }
        }
    }
}