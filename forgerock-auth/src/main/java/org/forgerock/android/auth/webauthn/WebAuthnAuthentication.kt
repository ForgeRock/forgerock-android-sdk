/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.webauthn

import android.content.Context
import android.util.Base64
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import kotlinx.coroutines.tasks.await
import org.forgerock.android.auth.WebAuthnDataRepository
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Handle WebAuthn Authentication
 */
open class WebAuthnAuthentication() : WebAuthn() {

    internal lateinit var options: PublicKeyCredentialRequestOptions

    constructor(input: JSONObject) : this() {
        val challenge = Base64.decode(input.getString(CHALLENGE), Base64.NO_WRAP)
        val relayingPartyId = getRelyingPartyId(input)
        val timeout = input.optString(TIMEOUT, TIMEOUT_DEFAULT).toDouble() / 1000
        val allowCredentials = getAllowCredentials(input)
        //val userVerification = input.optString(USER_VERIFICATION, null)
        options = PublicKeyCredentialRequestOptions.Builder()
            .setAllowList(allowCredentials)
            .setRpId(relayingPartyId)
            .setChallenge(challenge)
            .setTimeoutSeconds(timeout)
            .build()
    }

    constructor(options: PublicKeyCredentialRequestOptions) : this() {
        this.options = options
    }

    /**
     * Parse and retrieve all the allow credentials
     *
     * @param value The json from WebAuthn Authentication Node
     * @return The parsed PublicKeyCredentialDescriptor
     * @throws JSONException Failed to parse the Json
     */
    @Throws(JSONException::class)
    protected fun getAllowCredentials(value: JSONObject): List<PublicKeyCredentialDescriptor> {
        var allowCredentials = JSONArray()
        if (value.has(_ALLOW_CREDENTIALS)) {
            allowCredentials = value.getJSONArray(_ALLOW_CREDENTIALS)
        } else if (value.has(ALLOW_CREDENTIALS)) {
            val allowCredentialString = value.getString(ALLOW_CREDENTIALS)
                .replace("(allowCredentials: |new Int8Array\\(|\\).buffer )".toRegex(), "")
            if (allowCredentialString.trim().isNotEmpty()) {
                allowCredentials = JSONArray(allowCredentialString)
            }
        }
        return getCredentials(allowCredentials)
    }

    /**
     * Perform WebAuthn Authentication
     *
     * @param context             The Application Context
     * @param fragmentManager     The FragmentManager to manage the lifecycle of Fido API Callback
     * @param webAuthnKeySelector The Selector for user to select which credential to use (UsernameLess)
     * @param listener            The Listener for the result event.
     */
    suspend fun authenticate(context: Context,
                             webAuthnKeySelector: WebAuthnKeySelector = WebAuthnKeySelector.DEFAULT): String {

        var userHandle: ByteArray? = null
        //username less when allowCredentials is empty
        if (options.allowList.isNullOrEmpty()) {
            //TODO What about username less with passkey?
            val publicKeyCredentialSources = getPublicKeyCredentialSource(context)
            //When there is only one stored credential, automatically trigger with the stored credential.
            if (publicKeyCredentialSources.isNotEmpty()) {
                //Launch a dialog and ask for which user for authentication
                webAuthnKeySelector.select(publicKeyCredentialSources)?.let {
                    options = options.cloneWith(listOf(it.toDescriptor()))
                    userHandle = it.userHandle
                }
            }
        }
        return authenticate(context, userHandle)
    }

    /**
     * Retrieve the [PublicKeyCredentialSource]
     *
     * @param context The Application Context
     * @return The stored [PublicKeyCredentialSource]
     */
    internal open fun getPublicKeyCredentialSource(context: Context): List<PublicKeyCredentialSource> {
        return WebAuthnDataRepository.builder()
            .context(context).build()
            .getPublicKeyCredentialSource(options.rpId)
    }

    private suspend fun authenticate(context: Context, userHandle: ByteArray?): String {
        val publicKeyCredential = getPublicKeyCredential(context)
        val response = publicKeyCredential.response as AuthenticatorAssertionResponse
        val sb = StringBuilder()
        sb.append(String(response.clientDataJSON))
            .append("::")
            .append(format(response.authenticatorData))
            .append("::")
            .append(format(response.signature))
            .append("::")
            .append(Base64.encodeToString(publicKeyCredential.rawId,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        if (userHandle != null) {
            sb.append("::");
            sb.append(Base64.encodeToString(userHandle, Base64.URL_SAFE or Base64.NO_WRAP))
        } else {
            if (response.userHandle != null) {
                sb.append("::");
                val decoded = Base64.decode(response.userHandle, Base64.DEFAULT)
                sb.append(Base64.encodeToString(decoded, Base64.URL_SAFE or Base64.NO_WRAP))
            }
        }
        return (sb.toString())
    }

    override suspend fun getPublicKeyCredential(context: Context): PublicKeyCredential {
        val fido2ApiClient = Fido.getFido2ApiClient(context)
        val task = fido2ApiClient.getSignPendingIntent(options)
        //this may throw exception
        return WebAuthnFragment.launch(pendingIntent = task.await())
    }


}