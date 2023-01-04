/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.BaseTest
import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MetadataCallbackTest : BaseTest() {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "MetadataCallback",
            "output": [
                {
                    "name": "data",
                    "value": {
                        "stage": "UsernamePassword"
                    }
                }
            ],
            "_id": 0
        }""")
        val callback = MetadataCallback(raw, 0)
        Assertions.assertThat(callback.value).isInstanceOf(
            JSONObject::class.java)
        Assertions.assertThat(callback.value.getString("stage")).isEqualTo("UsernamePassword")
        Assertions.assertThat(callback.get_id()).isEqualTo(0)
    }

    @Test
    @Throws(JSONException::class)
    fun testDerivedCallbackRegistration71() {
        val callback = MetadataCallback(JSONObject(getJson("/webAuthn_registration_71.json"))
            .getJSONArray("callbacks").getJSONObject(0), 0)
        Assertions.assertThat(callback.derivedCallback).isAssignableFrom(
            WebAuthnRegistrationCallback::class.java)
    }

    @Test
    @Throws(JSONException::class)
    fun testDerivedCallbackAuthentication71() {
        val callback = MetadataCallback(JSONObject(getJson("/webAuthn_authentication_71.json"))
            .getJSONArray("callbacks").getJSONObject(0), 0)
        Assertions.assertThat(callback.derivedCallback).isAssignableFrom(
            WebAuthnAuthenticationCallback::class.java)
    }

    @Test
    @Throws(JSONException::class)
    fun testDerivedCallbackUndefined() {
        val raw = JSONObject("""{
            "type": "MetadataCallback",
            "output": [
                {
                    "name": "data",
                    "value": {
                        "stage": "UsernamePassword"
                    }
                }
            ],
            "_id": 0
        }""")
        val callback = MetadataCallback(raw, 0)
        Assertions.assertThat(callback.derivedCallback).isNull()
    }
}