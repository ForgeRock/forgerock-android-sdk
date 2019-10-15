/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AccessTokenTest {

    @Test
    public void accessTokenParser() {
        Date expiration = new Date(1565825948754L);
        AccessToken accessToken = AccessToken.builder()
                .expiration(expiration)
                .expiresIn(30)
                .idToken("myIdToken")
                .tokenType("myTokenType")
                .refreshToken("myRefreshToken")
                .value("MyAccessToken")
                .scope(AccessToken.Scope.parse("test1 test2"))
                .build();

        String json = accessToken.toJson();
        assertEquals("{\"value\":\"MyAccessToken\",\"expiresIn\":30,\"refreshToken\":\"myRefreshToken\",\"idToken\":\"myIdToken\",\"tokenType\":\"myTokenType\",\"scope\":[\"test2\",\"test1\"],\"expiration\":1565825948754}", json);

        AccessToken newAccessToken = AccessToken.fromJson(json);
        assertEquals(accessToken.getValue(), newAccessToken.getValue());
        assertEquals(accessToken.getExpiration(), newAccessToken.getExpiration());
        assertEquals(accessToken.getExpiresIn(), newAccessToken.getExpiresIn());
        assertEquals(accessToken.getIdToken(), newAccessToken.getIdToken());
        assertEquals(accessToken.getTokenType(), newAccessToken.getTokenType());
        assertEquals(accessToken.getRefreshToken(), newAccessToken.getRefreshToken());
        assertEquals(accessToken.getScope(), newAccessToken.getScope());

    }

    @Test
    public void parseTest() {
        AccessToken.Scope scope = AccessToken.Scope.parse("openid email profile");
        assertEquals(3, scope.size());
        assertTrue(scope.contains("openid"));
        assertTrue(scope.contains("email"));
        assertTrue(scope.contains("profile"));
    }
}
