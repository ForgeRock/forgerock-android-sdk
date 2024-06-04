/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.webauthn

import android.content.Context
import android.os.Build
import android.util.Base64
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.Attachment
import com.google.android.gms.fido.fido2.api.common.Attachment.UnsupportedAttachmentException
import com.google.android.gms.fido.fido2.api.common.AttestationConveyancePreference
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.forgerock.android.auth.WebAuthnDataRepository
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Handle WebAuthn Registration
 */
open class WebAuthnRegistration() : WebAuthn() {

    internal lateinit var options: PublicKeyCredentialCreationOptions

    constructor(input: JSONObject) : this() {
        val challenge = Base64.decode(input.getString(CHALLENGE), Base64.NO_WRAP)
        val attestationPreference = AttestationConveyancePreference.fromString(input.optString(
            ATTESTATION_PREFERENCE,
            "none"))
        val userName = input.getString(USER_NAME)
        val userId = input.getString(USER_ID)
        val relyingPartyName = input.getString(RELYING_PARTY_NAME)
        val authenticatorSelection = getAuthenticatorSelectionCriteria(input)
        val pubKeyCredParams = getPublicKeyCredentialParameters(input)
        val timeout = input.optString(TIMEOUT, TIMEOUT_DEFAULT).toDouble() / 1000
        val excludeCredentials = getExcludeCredentials(input)
        val displayName = input.getString(DISPLAY_NAME)
        val relyingPartyId = getRelyingPartyId(input)

        options = PublicKeyCredentialCreationOptions.Builder()
            .setRp(PublicKeyCredentialRpEntity(relyingPartyId, relyingPartyName, null))
            .setAttestationConveyancePreference(attestationPreference)
            .setUser(PublicKeyCredentialUserEntity(userId.toByteArray(),
                userName,
                "",
                displayName)).setChallenge(challenge).setTimeoutSeconds(timeout)
            .setAuthenticatorSelection(authenticatorSelection)
            .setExcludeList(excludeCredentials).setParameters(pubKeyCredParams).build()
    }

    constructor(options: PublicKeyCredentialCreationOptions) : this() {
        this.options = options
    }

    /**
     * Parse and retrieve all the Public key credentials
     *
     * @param value The json from WebAuthn Registration Node
     * @return The parsed PublicKeyCredentialParameters
     * @throws JSONException Failed to parse the Json
     */
    @Throws(JSONException::class)
    protected fun getPublicKeyCredentialParameters(value: JSONObject): List<PublicKeyCredentialParameters> {
        val result: MutableList<PublicKeyCredentialParameters> = ArrayList()
        val pubKeyCredParams: JSONArray = if (value.has(_PUB_KEY_CRED_PARAMS)) {
            value.getJSONArray(_PUB_KEY_CRED_PARAMS)
        } else {
            JSONArray(value.getString(PUB_KEY_CRED_PARAMS))
        }
        for (i in 0 until pubKeyCredParams.length()) {
            val o = pubKeyCredParams.getJSONObject(i)
            result.add(PublicKeyCredentialParameters(o.getString(TYPE), o.getInt(ALG)))
        }
        return result
    }

    /**
     * Parse and retrieve AuthenticatorSelectionCriteria attribute
     *
     * @param value The json from WebAuthn Registration Node
     * @return The parsed AuthenticatorSelectionCriteria.
     * @throws JSONException Failed to parse the Json
     */
    @Throws(JSONException::class)
    protected fun getAuthenticatorSelectionCriteria(value: JSONObject): AuthenticatorSelectionCriteria {
        val authenticatorSelection = if (value.has(_AUTHENTICATOR_SELECTION)) {
            value.getJSONObject(_AUTHENTICATOR_SELECTION)
        } else {
            JSONObject(value.getString(AUTHENTICATOR_SELECTION))
        }
        val isRequireResidentKey = authenticatorSelection.optBoolean(REQUIRE_RESIDENT_KEY, false)

        var attachment = Attachment.PLATFORM
        if (authenticatorSelection.has(AUTHENTICATOR_ATTACHMENT)) {
            attachment =
                Attachment.fromString(authenticatorSelection.getString(AUTHENTICATOR_ATTACHMENT))
            if (attachment == Attachment.CROSS_PLATFORM) {
                throw UnsupportedAttachmentException("Cross Platform attachment is not supported")
            }
        }

        //Passkeys are only supported on Android P+
        val requirement =
            if (isRequireResidentKey and (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P))
                ResidentKeyRequirement.RESIDENT_KEY_REQUIRED else ResidentKeyRequirement.RESIDENT_KEY_DISCOURAGED

        return AuthenticatorSelectionCriteria.Builder().setAttachment(attachment)
            .setRequireResidentKey(isRequireResidentKey)
            .setResidentKeyRequirement(requirement)
            .build()
    }

    /**
     * Parse and retrieve PublicKeyCredentialDescriptor attribute
     *
     * @param value The json from WebAuthn Registration Node
     * @return The parsed PublicKeyCredentialDescriptor.
     * @throws JSONException Failed to parse the Json
     */
    @Throws(JSONException::class)
    protected fun getExcludeCredentials(value: JSONObject): List<PublicKeyCredentialDescriptor> {
        val excludeCredentials: JSONArray = if (value.has(_EXCLUDE_CREDENTIALS)) {
            value.getJSONArray(_EXCLUDE_CREDENTIALS)
        } else {
            val excludeCredentialString = ("[" + value.optString(EXCLUDE_CREDENTIALS,
                "") + "]").replace("(new Int8Array\\(|\\).buffer )".toRegex(), "")
            JSONArray(excludeCredentialString)
        }
        return getCredentials(excludeCredentials)
    }

    suspend fun register(context: Context): String {
        val publicKeyCredential = getPublicKeyCredential(context)
        val response = publicKeyCredential.response as AuthenticatorAttestationResponse
        val sb = StringBuilder()
        sb.append(String(response.clientDataJSON))
            .append("::")
            .append(format(response.attestationObject))
            .append("::")
            .append(Base64.encodeToString(
                publicKeyCredential.rawId,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))

        //Extension to support username-less
        if (options.authenticatorSelection?.requireResidentKey == true &&
            options.authenticatorSelection?.residentKeyRequirement == ResidentKeyRequirement.RESIDENT_KEY_DISCOURAGED) {
            val source = PublicKeyCredentialSource.builder()
                .id(publicKeyCredential.rawId)
                .rpid(options.rp.id)
                .userHandle(Base64.decode(options.user.id, Base64.URL_SAFE or Base64.NO_WRAP))
                .otherUI(options.user.displayName).build()
            persist(context, source)
        }
        return (sb.toString())
    }

    /**
     * Persist the [PublicKeyCredentialSource]
     *
     * @param context The Application context
     * @param source  The [PublicKeyCredentialSource] to persist
     */
    protected open suspend fun persist(context: Context, source: PublicKeyCredentialSource) =
        withContext(Dispatchers.IO) {
            WebAuthnDataRepository.builder().context(context).build().persist(source)
        }

    override suspend fun getPublicKeyCredential(context: Context): PublicKeyCredential {
        val fido2ApiClient = Fido.getFido2ApiClient(context)
        val task = fido2ApiClient.getRegisterPendingIntent(options)
        return WebAuthnFragment.launch(pendingIntent = task.await())
    }
}