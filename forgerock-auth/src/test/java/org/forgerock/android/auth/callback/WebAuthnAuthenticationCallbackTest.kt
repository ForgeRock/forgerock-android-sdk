/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.*
import org.forgerock.android.auth.FRListenerFuture
import org.forgerock.android.auth.webauthn.WebAuthnAuthentication
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WebAuthnAuthenticationCallbackTest  {

    val context: Context = ApplicationProvider.getApplicationContext()
    private val webAuthnAuthentication = mock<WebAuthnAuthentication>()

    fun getJson(path: String): String =
        IOUtils.toString(javaClass.getResourceAsStream(path), StandardCharsets.UTF_8)

    @Test
    fun testSuccessWithSuspendFunction() = runTest {
        val nodeListener = DummyNodeListener()
        val node = nodeListener.onCallbackReceived("",
            JSONObject(getJson("/webAuthn_authentication_71.json")))
        val callback = node.getCallback(WebAuthnAuthenticationCallback::class.java)
        assertThat(callback).isNotNull
        assertThat(node.getCallback(HiddenValueCallback::class.java)).isNotNull
        assertThat(callback.getWebAuthnAuthentication()).isNotNull

        val spCallback = spy(callback)
        doReturn(webAuthnAuthentication).`when`(spCallback).getWebAuthnAuthentication()
        whenever(webAuthnAuthentication.authenticate(any(), anyOrNull()
        )).thenReturn("SuccessResult")
        spCallback.authenticate(context, node)
        val hiddenValueCallback = node.getCallback(HiddenValueCallback::class.java)
        assertThat(hiddenValueCallback.contentAsJson.getJSONArray("input").getJSONObject(0)
            .getString("value")).isEqualTo("SuccessResult")
    }

    @Test
    fun testExceptionWithSuspendFunction() = runTest {
        val nodeListener = DummyNodeListener()
        val node = nodeListener.onCallbackReceived("",
            JSONObject(getJson("/webAuthn_authentication_71.json")))
        val callback = node.getCallback(WebAuthnAuthenticationCallback::class.java)

        val spCallback = spy(callback)
        doThrow(JSONException("Invalid Format")).`when`(spCallback).getWebAuthnAuthentication()
        try {
            spCallback.authenticate(context, node)
            failBecauseExceptionWasNotThrown(JSONException::class.java)
        } catch (e: JSONException) {
            assertThat(e).isInstanceOf(JSONException::class.java)
        }
        val hiddenValueCallback = node.getCallback(HiddenValueCallback::class.java)
        assertThat(hiddenValueCallback.contentAsJson.getJSONArray("input").getJSONObject(0)
            .getString("value")).isEqualTo("ERROR::UnknownError:Invalid Format")
    }

    @Test
    fun testSuccessWithCallbackFunction() = runTest {
        val nodeListener = DummyNodeListener()
        val node = nodeListener.onCallbackReceived("",
            JSONObject(getJson("/webAuthn_authentication_71.json")))
        val callback = node.getCallback(WebAuthnAuthenticationCallback::class.java)
        assertThat(callback).isNotNull
        assertThat(node.getCallback(HiddenValueCallback::class.java)).isNotNull
        assertThat(callback.getWebAuthnAuthentication()).isNotNull

        val spCallback = spy(callback)
        doReturn(webAuthnAuthentication).`when`(spCallback).getWebAuthnAuthentication()
        whenever(webAuthnAuthentication.authenticate(any(), any())).thenReturn("SuccessResult")
        val future = FRListenerFuture<Void>()
        spCallback.authenticate(context, node, listener = future)
        future.get()
        val hiddenValueCallback = node.getCallback(HiddenValueCallback::class.java)
        assertThat(hiddenValueCallback.contentAsJson.getJSONArray("input").getJSONObject(0)
            .getString("value")).isEqualTo("SuccessResult")
    }

}

