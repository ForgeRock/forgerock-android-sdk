/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.webauthn

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.*
import org.forgerock.android.auth.BaseTest
import org.forgerock.android.auth.WebAuthnDataRepository
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.*

@RunWith(AndroidJUnit4::class)
class WebAuthnRegistrationTest : BaseTest() {
    private lateinit var webAuthnRegistration: WebAuthnRegistration
    private var publicKeyCredential = mock<PublicKeyCredential>()
    private var authenticatorAttestationResponse = mock<AuthenticatorAttestationResponse>()

    @Test
    fun testParsingParameter71() {
        val value = JSONObject(getJson("/webAuthn_registration_71.json"))
            .getJSONArray("callbacks")
            .getJSONObject(0)
            .getJSONArray("output")
            .getJSONObject(0)
            .getJSONObject("value")
        testParsingParameter(value)
    }

    @Test
    fun testRegistrationWithUsernameless(): Unit = runBlocking {

        val value = JSONObject(getJson("/webAuthn_registration_71.json"))
            .getJSONArray("callbacks")
            .getJSONObject(0)
            .getJSONArray("output")
            .getJSONObject(0)
            .getJSONObject("value")

        val sharedPreferences = context.getSharedPreferences("Test", Context.MODE_PRIVATE)
        val repository: WebAuthnDataRepository =
            object : WebAuthnDataRepository(context, sharedPreferences) {
                override fun getNewSharedPreferences(): SharedPreferences {
                    return sharedPreferences
                }
            }

        whenever(publicKeyCredential.response).thenReturn(authenticatorAttestationResponse)
        whenever(publicKeyCredential.rawId).thenReturn("rawId".toByteArray())
        whenever(authenticatorAttestationResponse.attestationObject).thenReturn("attestationObject".toByteArray())
        whenever(authenticatorAttestationResponse.clientDataJSON).thenReturn("clientDataJson".toByteArray())

        //Override with discourage
        var webAuthnRegistration = WebAuthnRegistration(value)
        val options = webAuthnRegistration.options.cloneWith(ResidentKeyRequirement.RESIDENT_KEY_DISCOURAGED)

        webAuthnRegistration = object : WebAuthnRegistration(options) {
            override suspend fun getPublicKeyCredential(context: Context): PublicKeyCredential {
                return publicKeyCredential
            }
            override suspend fun persist(context: Context, source: PublicKeyCredentialSource) {
                repository.persist(source)
            }
        }
        val result = webAuthnRegistration.register(context)

        assertThat(result).isEqualTo("clientDataJson::97,116,116,101,115,116,97,116,105,111,110,79,98,106,101,99,116::cmF3SWQ")
        assertThat(repository.getPublicKeyCredentialSource("humorous-cuddly-carrot.glitch.me"))
            .hasSize(1)
    }



    private fun testParsingParameter(value: JSONObject) {
        webAuthnRegistration = WebAuthnRegistration(value)
        val options = webAuthnRegistration.options
        assertThat(options.attestationConveyancePreference!!.name).isEqualTo("NONE")
        assertThat(options.authenticationExtensions).isNull()
        //Google not allow to set the residentKey
        assertThat(options.authenticatorSelection!!.requireResidentKey).isTrue
        assertThat(options.authenticatorSelection!!.residentKeyRequirement).isEqualTo(
            ResidentKeyRequirement.RESIDENT_KEY_REQUIRED)
        assertThat(options.authenticatorSelection!!.attachment.toString())
            .isEqualTo("platform")
        val challenge = "X5OsmgG2We2Xgir575Grt19hwXoC9m7Jth6UxWOrEYE="
        assertThat(options.challenge).isEqualTo(Base64.getDecoder().decode(challenge))
        assertThat(options.timeoutSeconds).isEqualTo(60.0)
        assertThat(options.excludeList).hasSize(3)
        assertThat(options.parameters).hasSize(2)
        assertThat(options.parameters[0].type)
            .isEqualTo(PublicKeyCredentialType.PUBLIC_KEY)
        assertThat(options.parameters[0].algorithmIdAsInteger).isEqualTo(-7)
        assertThat(options.parameters[1].type)
            .isEqualTo(PublicKeyCredentialType.PUBLIC_KEY)
        assertThat(options.parameters[1].algorithmIdAsInteger).isEqualTo(-257)
        assertThat(options.rp.name).isEqualTo("ForgeRock")
        assertThat(options.rp.icon).isNull()
        assertThat(options.rp.id).isEqualTo("humorous-cuddly-carrot.glitch.me")
        assertThat(options.user.displayName)
            .isEqualTo("e24f0d7c-a9d5-4a3f-a002-6f808210a8a3")
        assertThat(options.user.name).isEqualTo("e24f0d7c-a9d5-4a3f-a002-6f808210a8a3")
        assertThat(options.user.icon).isEmpty()
        val id = "WlRJMFpqQmtOMk10WVRsa05TMDBZVE5tTFdFd01ESXRObVk0TURneU1UQmhPR0V6"
        assertThat(options.user.id).isEqualTo(Base64.getDecoder().decode(id))
        assertThat(options.tokenBinding).isNull()
        assertThat(options.requestId).isNull()
    }
}