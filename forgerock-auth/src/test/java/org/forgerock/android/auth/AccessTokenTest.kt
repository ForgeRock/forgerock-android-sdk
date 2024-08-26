/*
 * Copyright (c) 2023 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test
import java.util.Date

class AccessTokenTest {
    @Test
    fun accessTokenParser() {
        val expiration = Date(1565825948754L)
        val accessToken = AccessToken("MyAccessToken", "myTokenType", AccessToken.Scope().apply {
            add("test1")
            add("test2")
        }, 30, "myRefreshToken", "myIdToken", expiration, SSOToken("mySSOToken"))

        val json =  json.encodeToString(accessToken)
        Assert.assertEquals("{\"value\":\"MyAccessToken\",\"tokenType\":\"myTokenType\",\"scope\":[\"test2\",\"test1\"],\"expiresIn\":30,\"refreshToken\":\"myRefreshToken\",\"idToken\":\"myIdToken\",\"expiration\":1565825948754,\"sessionToken\":\"mySSOToken\"}",
            json)
        val newAccessToken = Json.decodeFromString<AccessToken>(json)
        Assert.assertEquals(accessToken.value, newAccessToken.value)
        Assert.assertEquals(accessToken.expiration, newAccessToken.expiration)
        Assert.assertEquals(accessToken.expiresIn, newAccessToken.expiresIn)
        Assert.assertEquals(accessToken.idToken, newAccessToken.idToken)
        Assert.assertEquals(accessToken.tokenType, newAccessToken.tokenType)
        Assert.assertEquals(accessToken.refreshToken, newAccessToken.refreshToken)
        Assert.assertEquals(accessToken.scope, newAccessToken.scope)
        Assert.assertEquals(accessToken.sessionToken, newAccessToken.sessionToken)
    }

    @Test
    fun parseTest() {
        val scope = AccessToken.Scope.parse("openid email profile")
        Assert.assertEquals(3, scope!!.size.toLong())
        Assert.assertTrue(scope.contains("openid"))
        Assert.assertTrue(scope.contains("email"))
        Assert.assertTrue(scope.contains("profile"))
    }
}