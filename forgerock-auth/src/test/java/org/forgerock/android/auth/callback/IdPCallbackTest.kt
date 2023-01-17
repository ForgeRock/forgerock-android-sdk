/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IdPCallbackTest {
    @Test
    @Throws(JSONException::class)
    fun basicTest() {
        val raw = JSONObject("""{
            "type": "IdPCallback",
            "output": [
                {
                    "name": "provider",
                    "value": "google"
                },
                {
                    "name": "clientId",
                    "value": "1234567.apps.googleusercontent.com"
                },
                {
                    "name": "redirectUri",
                    "value": "https://forgerock.org/oauth2redirect"
                },
                {
                    "name": "scopes",
                    "value": [
                        "openid",
                        "profile",
                        "email"
                    ]
                },
                {
                    "name": "nonce",
                    "value": ""
                },
                {
                    "name": "acrValues",
                    "value": [
                        "test",
                        "test2"
                    ]
                },
                {
                    "name": "request",
                    "value": "eyJ0eXAiOiJKV1QiLCJraWQiOiJ3VTNpZklJYUxPVUFSZVJCL0ZHNmVNMVAxUU09IiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20vby9vYXV0aDIvdjIvYXV0aD9wcm9tcHQ9bG9naW4iLCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwiYWNyX3ZhbHVlcyI6InRlc3QgdGVzdDIiLCJpc3MiOiI0MjIzODY0Mzg2MzItOWRpcjRtcDM5MWdmc2F0OG00Z2lxMGFrcGVpOTB0N2IuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJyZXNwb25zZV90eXBlIjoiY29kZSIsInJlZGlyZWN0X3VyaSI6Imh0dHBzOi8vZm9yZ2Vyb2NrLm9yZy9vYXV0aDJyZWRpcmVjdCIsInN0YXRlIjoiNTg2M2hvbmk2eDVqaDllZnc1cmdjMmZicG1iYWo1cCIsIm5vbmNlIjoiZndyb2twZmFxcmo1Ym84eHA5M3lqOGlsOHBmeGxneSIsImNsaWVudF9pZCI6IjQyMjM4NjQzODYzMi05ZGlyNG1wMzkxZ2ZzYXQ4bTRnaXEwYWtwZWk5MHQ3Yi5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSJ9.nmyVU7dPnXOo01f_u7k0lorKCD8dKiAGiTORpY-pbNsIA55PEHk4_d1jgI_6pdYsejQtiiIiUCAh4xiSPAVj9-bhGU2Tu1gi_xxmZFylLUtLIVQHMd6dPlqSz1-ujj3bd84O5_iv7VWX_lzooyYznr7FAi9Z5PwTkhGhklwxSgAT8Gj3AAjmOp8E1UUx1CveDYyHbTKqtg11p2Eh5_m2zx3q5B7Jfvt9tVAU9JLefF7N-zmMoSr6wdNZnitEUyNiDGedve1iViVeURLTHjKa_T0fM2d_A85vAHCeiJfx3RjcOWa9OGk5dEAucypix6wkNEl2wt-F-2sQ1C-kN4sntA"
                },
                {
                    "name": "requestUri",
                    "value": ""
                }
            ],
            "input": [
                {
                    "name": "IDToken1token",
                    "value": ""
                },
                {
                    "name": "IDToken1token_type",
                    "value": ""
                }
            ]
        }""")
        val callback = IdPCallback(raw, 0)
        Assertions.assertThat(callback.clientId).isEqualTo("1234567.apps.googleusercontent.com")
        Assertions.assertThat(callback.provider).isEqualTo("google")
        Assertions.assertThat(callback.redirectUri)
            .isEqualTo("https://forgerock.org/oauth2redirect")
        Assertions.assertThat(callback.scopes).contains("openid", "profile", "email")
        Assertions.assertThat(callback.nonce).isEqualTo("")
        Assertions.assertThat(callback.acrValues).contains("test", "test2")
        Assertions.assertThat(callback.request)
            .isEqualTo("eyJ0eXAiOiJKV1QiLCJraWQiOiJ3VTNpZklJYUxPVUFSZVJCL0ZHNmVNMVAxUU09IiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20vby9vYXV0aDIvdjIvYXV0aD9wcm9tcHQ9bG9naW4iLCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwiYWNyX3ZhbHVlcyI6InRlc3QgdGVzdDIiLCJpc3MiOiI0MjIzODY0Mzg2MzItOWRpcjRtcDM5MWdmc2F0OG00Z2lxMGFrcGVpOTB0N2IuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJyZXNwb25zZV90eXBlIjoiY29kZSIsInJlZGlyZWN0X3VyaSI6Imh0dHBzOi8vZm9yZ2Vyb2NrLm9yZy9vYXV0aDJyZWRpcmVjdCIsInN0YXRlIjoiNTg2M2hvbmk2eDVqaDllZnc1cmdjMmZicG1iYWo1cCIsIm5vbmNlIjoiZndyb2twZmFxcmo1Ym84eHA5M3lqOGlsOHBmeGxneSIsImNsaWVudF9pZCI6IjQyMjM4NjQzODYzMi05ZGlyNG1wMzkxZ2ZzYXQ4bTRnaXEwYWtwZWk5MHQ3Yi5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSJ9.nmyVU7dPnXOo01f_u7k0lorKCD8dKiAGiTORpY-pbNsIA55PEHk4_d1jgI_6pdYsejQtiiIiUCAh4xiSPAVj9-bhGU2Tu1gi_xxmZFylLUtLIVQHMd6dPlqSz1-ujj3bd84O5_iv7VWX_lzooyYznr7FAi9Z5PwTkhGhklwxSgAT8Gj3AAjmOp8E1UUx1CveDYyHbTKqtg11p2Eh5_m2zx3q5B7Jfvt9tVAU9JLefF7N-zmMoSr6wdNZnitEUyNiDGedve1iViVeURLTHjKa_T0fM2d_A85vAHCeiJfx3RjcOWa9OGk5dEAucypix6wkNEl2wt-F-2sQ1C-kN4sntA")
        Assertions.assertThat(callback.requestUri).isEqualTo("")
        callback.setTokenType("id_token")
        callback.setToken("1234567")
        Assert.assertEquals((callback.contentAsJson.getJSONArray("input")[0] as JSONObject).getString(
            "value"),
            "1234567")
        Assert.assertEquals((callback.contentAsJson.getJSONArray("input")[1] as JSONObject).getString(
            "value"),
            "id_token")
        Assert.assertEquals(0, callback.get_id().toLong())
    }
}