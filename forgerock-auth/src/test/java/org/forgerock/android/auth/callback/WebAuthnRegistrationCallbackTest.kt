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
import com.google.android.gms.fido.fido2.api.common.Attachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.*
import org.forgerock.android.auth.FRListenerFuture
import org.forgerock.android.auth.webauthn.WebAuthnRegistration
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WebAuthnRegistrationCallbackTest {

    val context: Context = ApplicationProvider.getApplicationContext()
    private val webAuthnRegistration = mock<WebAuthnRegistration>()

    fun getJson(path: String): String =
        IOUtils.toString(javaClass.getResourceAsStream(path), StandardCharsets.UTF_8)

    @Test
    fun testSuccessWithSuspendFunction() = runTest {
        val nodeListener = DummyNodeListener()
        val node = nodeListener.onCallbackReceived("",
            JSONObject(getJson("/webAuthn_registration_71.json")))
        val callback = node.getCallback(WebAuthnRegistrationCallback::class.java)
        assertThat(callback).isNotNull
        assertThat(node.getCallback(HiddenValueCallback::class.java)).isNotNull
        assertThat(callback.getWebAuthnRegistration()).isNotNull

        val spCallback = spy(callback)
        doReturn(webAuthnRegistration).`when`(spCallback).getWebAuthnRegistration()
        whenever(webAuthnRegistration.register(any())).thenReturn("SuccessResult")
        spCallback.register(context, node = node)
        val hiddenValueCallback = node.getCallback(HiddenValueCallback::class.java)
        assertThat(hiddenValueCallback.contentAsJson.getJSONArray("input").getJSONObject(0)
            .getString("value")).isEqualTo("SuccessResult")
    }

    @Test
    fun testExceptionWithSuspendFunction() = runTest {
        val nodeListener = DummyNodeListener()
        val node = nodeListener.onCallbackReceived("",
            JSONObject(getJson("/webAuthn_registration_71.json")))
        val callback = node.getCallback(WebAuthnRegistrationCallback::class.java)

        val spCallback = spy(callback)
        doThrow(Attachment.UnsupportedAttachmentException::class.java).`when`(spCallback)
            .getWebAuthnRegistration()
        try {
            spCallback.register(context, node = node)
            failBecauseExceptionWasNotThrown(Attachment.UnsupportedAttachmentException::class.java)
        } catch (e: Attachment.UnsupportedAttachmentException) {
            assertThat(e).isInstanceOf(Attachment.UnsupportedAttachmentException::class.java)
        }
        val hiddenValueCallback = node.getCallback(HiddenValueCallback::class.java)
        assertThat(hiddenValueCallback.contentAsJson.getJSONArray("input").getJSONObject(0)
            .getString("value")).isEqualTo("unsupported")
    }

    @Test
    fun testSuccessWithCallbackFunction() = runTest {
        val nodeListener = DummyNodeListener()
        val node = nodeListener.onCallbackReceived("",
            JSONObject(getJson("/webAuthn_registration_71.json")))
        val callback = node.getCallback(WebAuthnRegistrationCallback::class.java)
        assertThat(callback).isNotNull
        assertThat(node.getCallback(HiddenValueCallback::class.java)).isNotNull
        assertThat(callback.getWebAuthnRegistration()).isNotNull

        val spCallback = spy(callback)
        doReturn(webAuthnRegistration).`when`(spCallback).getWebAuthnRegistration()
        whenever(webAuthnRegistration.register(any())).thenReturn("SuccessResult")
        val future = FRListenerFuture<Void>()
        spCallback.register(context, node = node, listener = future)
        future.get()
        val hiddenValueCallback = node.getCallback(HiddenValueCallback::class.java)
        assertThat(hiddenValueCallback.contentAsJson.getJSONArray("input").getJSONObject(0)
            .getString("value")).isEqualTo("SuccessResult")
    }

    @Test
    fun `Test device name append to the hidden value result`()  = runTest {
        val nodeListener = DummyNodeListener()
        val node = nodeListener.onCallbackReceived("",
            JSONObject(getJson("/webAuthn_registration_71.json")))
        val callback = node.getCallback(WebAuthnRegistrationCallback::class.java)
        val spCallback = spy(callback)
        doReturn(webAuthnRegistration).`when`(spCallback).getWebAuthnRegistration()
        whenever(webAuthnRegistration.register(any())).thenReturn("SuccessResult")
        spCallback.register(context, "MyDeviceName", node)
        val hiddenValueCallback = node.getCallback(HiddenValueCallback::class.java)
        assertThat(hiddenValueCallback.contentAsJson.getJSONArray("input").getJSONObject(0)
            .getString("value")).isEqualTo("SuccessResult::MyDeviceName")
    }

}

