/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.annotation.TargetApi
import android.content.Context
import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.Listener
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.webauthn.WebAuthn
import org.forgerock.android.auth.webauthn.WebAuthnRegistration
import org.forgerock.android.auth.webauthn.cloneWith
import org.json.JSONException
import org.json.JSONObject

/**
 * A callback that handle WebAuthnRegistration Node.
 */
@TargetApi(24)
open class WebAuthnRegistrationCallback : MetadataCallback, WebAuthnCallback {

    @JvmOverloads
    constructor(jsonObject: JSONObject, index: Int) : super(jsonObject, index)

    @JvmOverloads
    constructor() : super()

    override fun getType(): String {
        return "WebAuthnRegistrationCallback"
    }

    private var residentKeyRequirement: String? = null

    open fun setResidentKeyRequirement(requirement: ResidentKeyRequirement) {
        residentKeyRequirement = requirement.name
    }

    /**
     * Perform WebAuthn Registration
     *
     * @param context The application context
     * @param node The current Node
     */
    suspend fun register(context: Context, node: Node) {
        try {
            val webAuthnRegistration = getWebAuthnRegistration()
            //Override the ResidentKeyRequirement
            residentKeyRequirement?.let {
                webAuthnRegistration.options = webAuthnRegistration.options.cloneWith(
                    ResidentKeyRequirement.valueOf(it))
            }
            val result = webAuthnRegistration.register(context)
            setHiddenCallbackValue(node, result);
        } catch (e: Exception) {
            setErrorRethrow(node, e)
        }
    }

    /**
     * Perform WebAuthn Registration
     *
     * @param node     The current Node
     * @param listener Listener to listen for WebAuthn Registration Event
     */
    fun register(context: Context, node: Node,
                 listener: FRListener<Void>) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                register(context, node)
                Listener.onSuccess(listener, null)
            } catch (e: Exception) {
                Listener.onException(listener, e)
            }
        }
    }


    @Throws(Exception::class)
    open fun getWebAuthnRegistration() = WebAuthnRegistration(value)

    companion object {
        /**
         * Check if this callback is [WebAuthnRegistrationCallback] Type
         *
         * @param value The callback raw data json.
         * @return True if this is a [WebAuthnRegistrationCallback] Type, else false
         */
        @JvmStatic
        fun instanceOf(value: JSONObject): Boolean {
            //_action is provided AM version >= AM 7.1
            if (value.has(WebAuthn._ACTION)) {
                try {
                    if (value.getString(WebAuthn._ACTION) == WebAuthn.WEBAUTHN_REGISTRATION) {
                        return true
                    }
                } catch (e: JSONException) {
                    //Should not happened
                    return false
                }
            }
            return try {
                value.has(WebAuthn._TYPE) && value.getString(WebAuthn._TYPE) == WebAuthn.WEB_AUTHN && (value.has(
                    WebAuthn.PUB_KEY_CRED_PARAMS) || value.has(WebAuthn._PUB_KEY_CRED_PARAMS))
            } catch (e: JSONException) {
                false
            }
        }
    }
}