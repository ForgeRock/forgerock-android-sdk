/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class AccessTokenTest {
    @Test
    fun accessTokenParser() {
        val expiration = Date(1565825948754L)
        val accessToken = AccessToken.builder()
            .expiration(expiration)
            .expiresIn(30)
            .idToken("myIdToken")
            .tokenType("myTokenType")
            .refreshToken("myRefreshToken")
            .value("MyAccessToken")
            .scope(AccessToken.Scope.parse("test1 test2"))
            .build()
        val json = accessToken.toJson()
        Assert.assertEquals("{\"value\":\"MyAccessToken\",\"expiresIn\":30,\"refreshToken\":\"myRefreshToken\",\"idToken\":\"myIdToken\",\"tokenType\":\"myTokenType\",\"scope\":[\"test2\",\"test1\"],\"expiration\":1565825948754}",
            json)
        val newAccessToken = AccessToken.fromJson(json)
        Assert.assertEquals(accessToken.value, newAccessToken.value)
        Assert.assertEquals(accessToken.expiration, newAccessToken.expiration)
        Assert.assertEquals(accessToken.expiresIn, newAccessToken.expiresIn)
        Assert.assertEquals(accessToken.idToken, newAccessToken.idToken)
        Assert.assertEquals(accessToken.tokenType, newAccessToken.tokenType)
        Assert.assertEquals(accessToken.refreshToken, newAccessToken.refreshToken)
        Assert.assertEquals(accessToken.scope, newAccessToken.scope)
    }

    @Test
    fun parseTest() {
        val scope = AccessToken.Scope.parse("openid email profile")
        Assert.assertEquals(3, scope.size.toLong())
        Assert.assertTrue(scope.contains("openid"))
        Assert.assertTrue(scope.contains("email"))
        Assert.assertTrue(scope.contains("profile"))
    }
}