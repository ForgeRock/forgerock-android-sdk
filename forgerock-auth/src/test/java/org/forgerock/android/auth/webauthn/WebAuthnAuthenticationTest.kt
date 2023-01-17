/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.webauthn

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.forgerock.android.auth.WebAuthnDataRepository
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.nio.charset.StandardCharsets
import java.util.*

@RunWith(AndroidJUnit4::class)
class WebAuthnAuthenticationTest {
    val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var webAuthnAuthentication: WebAuthnAuthentication
    private var publicKeyCredential = mock<PublicKeyCredential>()
    private var authenticatorAssertionResponse = mock<AuthenticatorAssertionResponse>()

    @Test
    fun testParsingParameterWithUsernameLess71() {
        testParsingParameterWithUsernameLess(get71Callback())
    }

    @Test
    fun testParsingParameterWithUsername71() {
        testParsingParameterWithUsername(get71WithUserCallback())
    }

    @Test
    fun testUsernameLessWith1CredentialSource(): Unit = runBlocking {

        val value = JSONObject(getJson("/webAuthn_authentication_71.json"))
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

        val source = PublicKeyCredentialSource.builder()
            .id("keyHandle".toByteArray())
            .otherUI("test")
            .rpid("humorous-cuddly-carrot.glitch.me")
            .userHandle("test".toByteArray())
            .type("public-key")
            .build()
        repository.persist(source)

        whenever(publicKeyCredential.response).thenReturn(authenticatorAssertionResponse)
        whenever(publicKeyCredential.rawId).thenReturn("rawId".toByteArray())
        whenever(authenticatorAssertionResponse.authenticatorData).thenReturn("authenticationData".toByteArray())
        whenever(authenticatorAssertionResponse.clientDataJSON).thenReturn("clientDataJson".toByteArray())
        whenever(authenticatorAssertionResponse.signature).thenReturn("signature".toByteArray())

        webAuthnAuthentication = object : WebAuthnAuthentication(value) {
            override fun getPublicKeyCredentialSource(context: Context): List<PublicKeyCredentialSource> {
                return repository.getPublicKeyCredentialSource(source.rpid)
            }

            override suspend fun getPublicKeyCredential(context: Context): PublicKeyCredential {
                return publicKeyCredential
            }
        }
        val result = webAuthnAuthentication.authenticate(context, object : WebAuthnKeySelector {
            override suspend fun select(sourceList: List<PublicKeyCredentialSource>): PublicKeyCredentialSource {
                return sourceList.first()
            }
        })

        assertThat(result).isEqualTo("clientDataJson::97,117,116,104,101,110,116,105,99,97,116,105,111,110,68,97,116,97::115,105,103,110,97,116,117,114,101::cmF3SWQ::dGVzdA==")
    }

    private fun testParsingParameterWithUsernameLess(input: JSONObject) {
        webAuthnAuthentication = WebAuthnAuthentication(input)
        val options = webAuthnAuthentication.options
        assertThat(options.getAllowList()).isEmpty()
        assertThat(options.getRpId()).isEqualTo("humorous-cuddly-carrot.glitch.me") //TODO
        assertThat(options.getAuthenticationExtensions()).isNull()
        val challenge = "qnMsxgya8h6mUc6OyRu8jJ6Oq16tHV3cgE7juXGMDbg="
        assertThat(options.getChallenge()).isEqualTo(Base64.getDecoder().decode(challenge))
        assertThat(options.getTimeoutSeconds()).isEqualTo(60.0)
        assertThat(options.getTokenBinding()).isNull()
        assertThat(options.getRequestId()).isNull()
    }

    private fun get71Callback(): JSONObject {
        return JSONObject(getJson("/webAuthn_authentication_71.json"))
            .getJSONArray("callbacks")
            .getJSONObject(0)
            .getJSONArray("output")
            .getJSONObject(0)
            .getJSONObject("value")
    }

    private fun get71WithUserCallback(): JSONObject {
        return JSONObject(getJson("/webAuthn_authentication_with_user_71.json"))
            .getJSONArray("callbacks")
            .getJSONObject(0)
            .getJSONArray("output")
            .getJSONObject(0)
            .getJSONObject("value")
    }


    private fun testParsingParameterWithUsername(value: JSONObject) {
        webAuthnAuthentication = WebAuthnAuthentication(value)
        val options = webAuthnAuthentication.options
        assertThat(options.allowList).hasSize(2)
        assertThat(options.allowList!![0].type)
            .isEqualTo(PublicKeyCredentialType.PUBLIC_KEY)
        assertThat(options.allowList!![0].id).isNotNull
        assertThat(options.allowList!![1].type)
            .isEqualTo(PublicKeyCredentialType.PUBLIC_KEY)
        assertThat(options.allowList!![1].id).isNotNull
        assertThat(options.rpId).isEqualTo("humorous-cuddly-carrot.glitch.me") //TODO
        assertThat(options.authenticationExtensions).isNull()
        val challenge = "qnMsxgya8h6mUc6OyRu8jJ6Oq16tHV3cgE7juXGMDbg="
        assertThat(options.challenge).isEqualTo(Base64.getDecoder().decode(challenge))
        assertThat(options.timeoutSeconds).isEqualTo(60.0)
        assertThat(options.tokenBinding).isNull()
        assertThat(options.requestId).isNull()
    }

    fun getJson(path: String): String =
        IOUtils.toString(javaClass.getResourceAsStream(path), StandardCharsets.UTF_8)

}