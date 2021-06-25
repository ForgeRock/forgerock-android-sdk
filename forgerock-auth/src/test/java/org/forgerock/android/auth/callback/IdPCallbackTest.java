/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class IdPCallbackTest {

    @Test
    public void basicTest() throws JSONException {
        JSONObject raw = new JSONObject("{\n" +
                "            \"type\": \"IdPCallback\",\n" +
                "            \"output\": [\n" +
                "                {\n" +
                "                    \"name\": \"provider\",\n" +
                "                    \"value\": \"google\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"clientId\",\n" +
                "                    \"value\": \"1234567.apps.googleusercontent.com\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"redirectUri\",\n" +
                "                    \"value\": \"https://forgerock.org/oauth2redirect\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"scopes\",\n" +
                "                    \"value\": [\n" +
                "                        \"openid\",\n" +
                "                        \"profile\",\n" +
                "                        \"email\"\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"nonce\",\n" +
                "                    \"value\": \"\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"acrValues\",\n" +
                "                    \"value\": [\n" +
                "                        \"test\",\n" +
                "                        \"test2\"\n" +
                "                    ]\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"request\",\n" +
                "                    \"value\": \"eyJ0eXAiOiJKV1QiLCJraWQiOiJ3VTNpZklJYUxPVUFSZVJCL0ZHNmVNMVAxUU09IiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20vby9vYXV0aDIvdjIvYXV0aD9wcm9tcHQ9bG9naW4iLCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwiYWNyX3ZhbHVlcyI6InRlc3QgdGVzdDIiLCJpc3MiOiI0MjIzODY0Mzg2MzItOWRpcjRtcDM5MWdmc2F0OG00Z2lxMGFrcGVpOTB0N2IuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJyZXNwb25zZV90eXBlIjoiY29kZSIsInJlZGlyZWN0X3VyaSI6Imh0dHBzOi8vZm9yZ2Vyb2NrLm9yZy9vYXV0aDJyZWRpcmVjdCIsInN0YXRlIjoiNTg2M2hvbmk2eDVqaDllZnc1cmdjMmZicG1iYWo1cCIsIm5vbmNlIjoiZndyb2twZmFxcmo1Ym84eHA5M3lqOGlsOHBmeGxneSIsImNsaWVudF9pZCI6IjQyMjM4NjQzODYzMi05ZGlyNG1wMzkxZ2ZzYXQ4bTRnaXEwYWtwZWk5MHQ3Yi5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSJ9.nmyVU7dPnXOo01f_u7k0lorKCD8dKiAGiTORpY-pbNsIA55PEHk4_d1jgI_6pdYsejQtiiIiUCAh4xiSPAVj9-bhGU2Tu1gi_xxmZFylLUtLIVQHMd6dPlqSz1-ujj3bd84O5_iv7VWX_lzooyYznr7FAi9Z5PwTkhGhklwxSgAT8Gj3AAjmOp8E1UUx1CveDYyHbTKqtg11p2Eh5_m2zx3q5B7Jfvt9tVAU9JLefF7N-zmMoSr6wdNZnitEUyNiDGedve1iViVeURLTHjKa_T0fM2d_A85vAHCeiJfx3RjcOWa9OGk5dEAucypix6wkNEl2wt-F-2sQ1C-kN4sntA\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"requestUri\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"input\": [\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1token\",\n" +
                "                    \"value\": \"\"\n" +
                "                },\n" +
                "                {\n" +
                "                    \"name\": \"IDToken1token_type\",\n" +
                "                    \"value\": \"\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }");
        IdPCallback callback = new IdPCallback(raw, 0);
        assertThat(callback.getClientId()).isEqualTo("1234567.apps.googleusercontent.com");
        assertThat(callback.getProvider()).isEqualTo("google");
        assertThat(callback.getRedirectUri()).isEqualTo("https://forgerock.org/oauth2redirect");
        assertThat(callback.getScopes()).contains("openid","profile", "email");
        assertThat(callback.getNonce()).isEqualTo("");
        assertThat(callback.getAcrValues()).contains("test", "test2");
        assertThat(callback.getRequest()).isEqualTo("eyJ0eXAiOiJKV1QiLCJraWQiOiJ3VTNpZklJYUxPVUFSZVJCL0ZHNmVNMVAxUU09IiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20vby9vYXV0aDIvdjIvYXV0aD9wcm9tcHQ9bG9naW4iLCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwiYWNyX3ZhbHVlcyI6InRlc3QgdGVzdDIiLCJpc3MiOiI0MjIzODY0Mzg2MzItOWRpcjRtcDM5MWdmc2F0OG00Z2lxMGFrcGVpOTB0N2IuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJyZXNwb25zZV90eXBlIjoiY29kZSIsInJlZGlyZWN0X3VyaSI6Imh0dHBzOi8vZm9yZ2Vyb2NrLm9yZy9vYXV0aDJyZWRpcmVjdCIsInN0YXRlIjoiNTg2M2hvbmk2eDVqaDllZnc1cmdjMmZicG1iYWo1cCIsIm5vbmNlIjoiZndyb2twZmFxcmo1Ym84eHA5M3lqOGlsOHBmeGxneSIsImNsaWVudF9pZCI6IjQyMjM4NjQzODYzMi05ZGlyNG1wMzkxZ2ZzYXQ4bTRnaXEwYWtwZWk5MHQ3Yi5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSJ9.nmyVU7dPnXOo01f_u7k0lorKCD8dKiAGiTORpY-pbNsIA55PEHk4_d1jgI_6pdYsejQtiiIiUCAh4xiSPAVj9-bhGU2Tu1gi_xxmZFylLUtLIVQHMd6dPlqSz1-ujj3bd84O5_iv7VWX_lzooyYznr7FAi9Z5PwTkhGhklwxSgAT8Gj3AAjmOp8E1UUx1CveDYyHbTKqtg11p2Eh5_m2zx3q5B7Jfvt9tVAU9JLefF7N-zmMoSr6wdNZnitEUyNiDGedve1iViVeURLTHjKa_T0fM2d_A85vAHCeiJfx3RjcOWa9OGk5dEAucypix6wkNEl2wt-F-2sQ1C-kN4sntA");
        assertThat(callback.getRequestUri()).isEqualTo("");

        callback.setTokenType("id_token");
        callback.setToken("1234567");
        assertEquals(((JSONObject)callback.getContentAsJson().getJSONArray("input").get(0)).getString("value"),
                "1234567");
        assertEquals(((JSONObject)callback.getContentAsJson().getJSONArray("input").get(1)).getString("value"),
                "id_token");


        assertEquals(0, callback.get_id());

    }
}