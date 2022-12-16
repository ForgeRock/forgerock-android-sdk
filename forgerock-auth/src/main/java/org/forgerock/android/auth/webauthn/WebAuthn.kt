/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.webauthn

import android.content.Context
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement
import org.forgerock.android.auth.iterator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Abstract class to provide common utilities method for [WebAuthnAuthentication] and
 * [WebAuthnRegistration]
 */
abstract class WebAuthn {

    /**
     * Parse the relaying party id.
     *
     * @param value The json value to parse
     * @return The relaying party id.
     * @throws JSONException Failed to parse the json input.
     */
    fun getRelyingPartyId(value: JSONObject): String {
        return if (value.has("_relyingPartyId")) {
            value.getString("_relyingPartyId")
        } else {
            value.getString("relyingPartyId").replace("(rpId: |\"|,)".toRegex(), "")
                .replace("(id: |\"|,)".toRegex(), "")
        }
    }

    /**
     * Parse the [PublicKeyCredentialDescriptor]
     *
     * @param credentials The json array value to parse.
     * @return The list of [PublicKeyCredentialDescriptor]
     * @throws JSONException Failed to parse the json input.
     */
    protected fun getCredentials(credentials: JSONArray): List<PublicKeyCredentialDescriptor> {
        val result = mutableListOf<PublicKeyCredentialDescriptor>()
        for (i in 0 until credentials.length()) {
            val excludeCredential = credentials.getJSONObject(i)
            val type = excludeCredential.getString("type")
            val id = excludeCredential.getJSONArray("id")
            val bytes = ByteArray(id.length())
            for (j in 0 until id.length()) {
                bytes[j] = id.getInt(j).toByte()
            }
            val descriptor =
                PublicKeyCredentialDescriptor(PublicKeyCredentialType.fromString(type)
                    .toString(), bytes, listOf(Transport.INTERNAL))
            result.add(descriptor)
        }
        return result
    }

    companion object {
        const val _ACTION = "_action"
        const val CHALLENGE = "challenge"
        const val TIMEOUT = "timeout"
        const val _ALLOW_CREDENTIALS = "_allowCredentials"
        const val ALLOW_CREDENTIALS = "allowCredentials"
        const val USER_VERIFICATION = "userVerification"
        const val ATTESTATION_PREFERENCE = "attestationPreference"
        const val USER_NAME = "userName"
        const val USER_ID = "userId"
        const val RELYING_PARTY_NAME = "relyingPartyName"
        const val DISPLAY_NAME = "displayName"
        const val _PUB_KEY_CRED_PARAMS = "_pubKeyCredParams"
        const val PUB_KEY_CRED_PARAMS = "pubKeyCredParams"
        const val TYPE = "type"
        const val ALG = "alg"
        const val _AUTHENTICATOR_SELECTION = "_authenticatorSelection"
        const val AUTHENTICATOR_SELECTION = "authenticatorSelection"
        const val REQUIRED = "required"
        const val REQUIRE_RESIDENT_KEY = "requireResidentKey"
        const val AUTHENTICATOR_ATTACHMENT = "authenticatorAttachment"
        const val _EXCLUDE_CREDENTIALS = "_excludeCredentials"
        const val EXCLUDE_CREDENTIALS = "excludeCredentials"
        const val TIMEOUT_DEFAULT = "60000"
        const val WEB_AUTHN = "WebAuthn"
        const val WEBAUTHN_REGISTRATION = "webauthn_registration"
        const val WEBAUTHN_AUTHENTICATION = "webauthn_authentication"
        const val _TYPE = "_type"

        /**
         * Format the bytes array to string.
         *
         * @param bytes The input bytes array
         * @return The string representation of the bytes array.
         */
        fun format(bytes: ByteArray): String {
            val result = StringBuilder()
            for (aByte in bytes) {
                result.append(aByte.toInt()).append(",")
            }
            result.deleteCharAt(result.length - 1)
            return result.toString()
        }
    }

    abstract suspend fun getPublicKeyCredential(context: Context): PublicKeyCredential
}

/**
 * Clone this [PublicKeyCredentialRequestOptions] and replace
 * [PublicKeyCredentialRequestOptions.getAllowList] with provided allowList
 */
fun PublicKeyCredentialRequestOptions.cloneWith(allowList: List<PublicKeyCredentialDescriptor>) =
    PublicKeyCredentialRequestOptions.Builder()
        .setAllowList(allowList)
        .setRpId(this.rpId)
        .setChallenge(this.challenge)
        .setTimeoutSeconds(this.timeoutSeconds)
        .setAuthenticationExtensions(this.authenticationExtensions)
        .setRequestId(this.requestId)
        .setTokenBinding(this.tokenBinding)
        .build()

fun PublicKeyCredentialCreationOptions.cloneWith(residentKeyRequirement: ResidentKeyRequirement): PublicKeyCredentialCreationOptions =
    PublicKeyCredentialCreationOptions.Builder()
        .setRp(this.rp)
        .setAttestationConveyancePreference(this.attestationConveyancePreference)
        .setUser(this.user)
        .setChallenge(this.challenge)
        .setTimeoutSeconds(this.timeoutSeconds)
        .setAuthenticatorSelection(AuthenticatorSelectionCriteria.Builder()
            .setAttachment(this.authenticatorSelection?.attachment)
            .setRequireResidentKey(authenticatorSelection?.requireResidentKey)
            .setResidentKeyRequirement(residentKeyRequirement)
            .build())
        .setExcludeList(this.excludeList)
        .setRequestId(this.requestId)
        .setTokenBinding(this.tokenBinding)
        .setParameters(this.parameters).build()
