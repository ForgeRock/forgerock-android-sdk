/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.Config
import org.forgerock.android.auth.FRListenerFuture
import org.forgerock.android.auth.KeyStoreManager
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.security.PublicKey

@RunWith(AndroidJUnit4::class)
class DeviceProfileCollectorCallbackTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Mock
    var keyStoreManager: KeyStoreManager? = null

    @Mock
    var publicKey: PublicKey? = null
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val encoded = "public key".toByteArray()
        Mockito.`when`(publicKey!!.encoded).thenReturn(encoded)
        Mockito.`when`(keyStoreManager!!.getIdentifierKey(ArgumentMatchers.any()))
            .thenReturn(publicKey)
        Config.getInstance().init(context, null)
        Config.getInstance().keyStoreManager = keyStoreManager
    }

    @Test
    @Throws(Exception::class)
    fun testMetadata() {
        val raw = JSONObject("""{
            "type": "DeviceProfileCallback",
            "output": [
                {
                    "name": "metadata",
                    "value": true
                },
                {
                    "name": "location",
                    "value": false
                },
                {
                    "name": "message",
                    "value": ""
                }
            ],
            "input": [
                {
                    "name": "IDToken1",
                    "value": ""
                }
            ]
        }""")
        val callback = DeviceProfileCallback(raw, 0)
        val result = FRListenerFuture<Void>()
        callback.execute(context, result)
        result.get()
        val content =
            (callback.contentAsJson.getJSONArray("input")[0] as JSONObject).getString("value")
        val contentAsJson = JSONObject(content)
        Assertions.assertThat(contentAsJson["identifier"]).isNotNull
        Assertions.assertThat(contentAsJson["metadata"]).isNotNull
    }

    @Test
    @Throws(Exception::class)
    fun testNoAttributesToCollect() {
        val raw = JSONObject("""{
            "type": "DeviceProfileCallback",
            "output": [
                {
                    "name": "metadata",
                    "value": false
                },
                {
                    "name": "location",
                    "value": false
                },
                {
                    "name": "message",
                    "value": ""
                }
            ],
            "input": [
                {
                    "name": "IDToken1",
                    "value": ""
                }
            ]
        }""")
        val callback = DeviceProfileCallback(raw, 0)
        val result = FRListenerFuture<Void>()
        callback.execute(context, result)
        result.get()
        val content =
            (callback.contentAsJson.getJSONArray("input")[0] as JSONObject).getString("value")
        val contentAsJson = JSONObject(content)
        Assertions.assertThat(contentAsJson["identifier"]).isNotNull
        Assertions.assertThat(contentAsJson.opt("metadata")).isNull()
        Assertions.assertThat(contentAsJson.opt("location")).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun testAttributesToCollect() {
        val raw = JSONObject("""{
            "type": "DeviceProfileCallback",
            "output": [
                {
                    "name": "metadata",
                    "value": true
                },
                {
                    "name": "location",
                    "value": true
                },
                {
                    "name": "message",
                    "value": "Test Message"
                }
            ],
            "input": [
                {
                    "name": "IDToken1",
                    "value": ""
                }
            ]
        }""")
        val callback = DeviceProfileCallback(raw, 0)
        Assertions.assertThat(callback.isMetadata).isTrue
        Assertions.assertThat(callback.isLocation).isTrue
        Assertions.assertThat(callback.message).isEqualTo("Test Message")
    }
}