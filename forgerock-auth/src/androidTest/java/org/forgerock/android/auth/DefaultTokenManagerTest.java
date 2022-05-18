/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.security.KeyStore;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DefaultTokenManagerTest extends AndroidBaseTest {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private TokenManager tokenManager;

    @Before
    public void setUp() {
        tokenManager = DefaultTokenManager.builder()
                .context(context).build();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        //Clean up the created keys
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        keyStore.deleteEntry(DefaultTokenManager.ORG_FORGEROCK_V_1_KEYS);

        //Clean up the created files
        Context context = ApplicationProvider.getApplicationContext();
        String filePath = context.getFilesDir().getParent() + "/shared_prefs/" + DefaultTokenManager.ORG_FORGEROCK_V_1_TOKENS + ".xml";
        File deletePrefFile = new File(filePath);
        deletePrefFile.delete();
    }

    @Test
    public void storeAccessToken() throws AuthenticationRequiredException, ExecutionException, InterruptedException {
        AccessToken accessToken = AccessToken.builder()
                .value("access token")
                .idToken("id token")
                .scope(AccessToken.Scope.parse("openid test"))
                .tokenType("Bearer")
                .refreshToken("refresh token")
                .expiresIn(100)
                .build();

        tokenManager.persist(accessToken);

        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        tokenManager.getAccessToken(null, future);
        AccessToken storedAccessToken = future.get();
        assertEquals("access token", storedAccessToken.getValue());
        assertEquals("id token", storedAccessToken.getIdToken());
        assertTrue(storedAccessToken.getScope().contains("openid"));
        assertTrue(storedAccessToken.getScope().contains("test"));
        assertEquals("Bearer", storedAccessToken.getTokenType());
        assertEquals("refresh token", storedAccessToken.getRefreshToken());

    }

    @Test(expected = AuthenticationRequiredException.class)
    public void clearAccessToken() throws Throwable {
        AccessToken accessToken = AccessToken.builder()
                .value("access token")
                .idToken("id token")
                .scope(AccessToken.Scope.parse("openid test"))
                .tokenType("Bearer")
                .refreshToken("refresh token")
                .expiresIn(100)
                .build();

        tokenManager.persist(accessToken);
        tokenManager.clear();

        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        tokenManager.getAccessToken(null, future);
        try {
            future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        } catch (InterruptedException e) {
            throw e;
        }

    }

}
