/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.recaptcha.RecaptchaAction
import com.google.android.recaptcha.RecaptchaClient
import junit.framework.TestCase.fail
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ReCaptchaEnterpriseCallbackTest {

    private val application: Application = ApplicationProvider.getApplicationContext()

    private lateinit var callback: ReCaptchaEnterpriseCallback

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val raw = JSONObject(
            """ {
            "type": "ReCaptchaEnterpriseCallback",
            "output": [
                {
                    "name": "recaptchaSiteKey",
                    "value": "6Lf3tbYUAAAAAEm78fAOFRKb-n1M67FDtmpczIBK"
                }
            ],
            "input": [
                {
                    "name": "IDToken1token",
                    "value": ""
                },
                  {
                    "name": "IDToken1action",
                    "value": ""
                },
                   {
                    "name": "IDToken1clientError",
                    "value": ""
                },
                 {
                    "name": "IDToken1payload",
                    "value": ""
                }
            ]
        }"""
        )
        callback = ReCaptchaEnterpriseCallback(raw, 0)
    }

    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        Assert.assertEquals(
            "6Lf3tbYUAAAAAEm78fAOFRKb-n1M67FDtmpczIBK",
            callback.reCaptchaSiteKey
        )
    }

    @Test
    fun testCaptchaSuccess() = runTest {
        // Arrange

            val recaptchaClient = Mockito.mock(RecaptchaClient::class.java)
            val recaptchaClientProvider = Mockito.mock(RecaptchaClientProvider::class.java)
            whenever(recaptchaClientProvider.fetchClient(any(), anyString())).thenReturn(
                recaptchaClient
            )
            whenever(recaptchaClientProvider.execute(any(), any<RecaptchaAction>(), anyLong())).thenReturn("test-token")

            callback.execute(application, action = "login", timeoutInMillis = 15000L, provider = recaptchaClientProvider)

            verify(recaptchaClientProvider).fetchClient(
                application,
                "6Lf3tbYUAAAAAEm78fAOFRKb-n1M67FDtmpczIBK"
            )
            verify(recaptchaClientProvider).execute(recaptchaClient, RecaptchaAction.custom("login"), 15000L)

            assert(callback.content.contains("test-token"))
            assert(callback.tokenResult == "test-token")
    }

    @Test
    fun testInvalidToken() = runTest {
        // Arrange
            val recaptchaClient = Mockito.mock(RecaptchaClient::class.java)
            val recaptchaClientProvider = Mockito.mock(RecaptchaClientProvider::class.java)
            whenever(recaptchaClientProvider.fetchClient(any(), anyString())).thenReturn(
                recaptchaClient
            )
            whenever(recaptchaClientProvider.execute(any(), any<RecaptchaAction>(), anyLong())).thenReturn(null)

            try {
                callback.execute(application, provider = recaptchaClientProvider)
                fail()
            } catch (e: Exception) {
                assert(e.message == "INVALID_CAPTCHA_TOKEN")
                assert(callback.content.contains("INVALID_CAPTCHA_TOKEN"))
            }

    }

    @Test
    fun testCustomAction() = runTest {
        // Arrange
        val recaptchaClient = Mockito.mock(RecaptchaClient::class.java)
        val recaptchaClientProvider = Mockito.mock(RecaptchaClientProvider::class.java)
        whenever(recaptchaClientProvider.fetchClient(any(), anyString())).thenReturn(
            recaptchaClient
        )
        whenever(recaptchaClientProvider.execute(any(), any<RecaptchaAction>(), anyLong())).thenReturn("test-token")

        callback.execute(application, "custom-action", provider = recaptchaClientProvider)

        verify(recaptchaClientProvider).fetchClient(
            application,
            "6Lf3tbYUAAAAAEm78fAOFRKb-n1M67FDtmpczIBK"
        )
        verify(recaptchaClientProvider).execute(recaptchaClient, RecaptchaAction.custom("custom-action"), 10000L)

        assert(callback.content.contains("test-token"))

    }

    @Test
    fun testPayloadAction() = runTest {
        // Arrange
        val recaptchaClient = Mockito.mock(RecaptchaClient::class.java)
        val recaptchaClientProvider = Mockito.mock(RecaptchaClientProvider::class.java)
        whenever(recaptchaClientProvider.fetchClient(any(), anyString())).thenReturn(
            recaptchaClient
        )
        whenever(recaptchaClientProvider.execute(any(), any<RecaptchaAction>(), anyLong())).thenReturn("test-token")

        callback.execute(application, "custom-action", 15000L, recaptchaClientProvider)

        callback.setPayload(JSONObject().put("key", "value"))

        verify(recaptchaClientProvider).fetchClient(
            application,
            "6Lf3tbYUAAAAAEm78fAOFRKb-n1M67FDtmpczIBK"
        )
        verify(recaptchaClientProvider).execute(recaptchaClient, RecaptchaAction.custom("custom-action"), 15000L)

        assert(callback.content.contains("test-token"))
        assert(callback.content.contains("key"))
        assert(callback.content.contains("value"))
        assert(callback.content.contains("custom-action"))
    }
}