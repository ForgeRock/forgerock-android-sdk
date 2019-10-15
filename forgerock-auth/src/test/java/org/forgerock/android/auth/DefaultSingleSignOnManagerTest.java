/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.forgerock.android.auth.exception.AuthenticationRequiredException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class DefaultSingleSignOnManagerTest {

    private SingleSignOnManager tokenManager;
    private Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        tokenManager = DefaultSingleSignOnManager.builder()
                .encryptor(new MockEncryptor())
                .context(context).build();
    }

    @After
    public void cleanup() throws Exception {
        tokenManager.clear();
    }

    @Test
    public void storeToken() throws AuthenticationRequiredException {
        SSOToken token = new SSOToken("MyTokenValue");

        tokenManager.persist(token);

        Token storedToken = tokenManager.getToken();
        assertEquals("MyTokenValue", storedToken.getValue());
    }

    @Test
    public void clearToken() throws AuthenticationRequiredException {
        SSOToken token = new SSOToken("MyTokenValue");
        tokenManager.persist(token);
        tokenManager.clear();

        Token storedToken = tokenManager.getToken();
        assertNull(storedToken);
    }

    @Test
    public void getTokenWithError() {
        tokenManager = DefaultSingleSignOnManager.builder()
                .encryptor(new Encryptor() {
                    @Override
                    public byte[] encrypt(byte[] clearText) {
                        return new byte[0];
                    }

                    @Override
                    public byte[] decrypt(byte[] encryptedData) {
                        throw new RuntimeException("Dummy");
                    }

                    @Override
                    public void reset() throws GeneralSecurityException, IOException {

                    }
                })
                .context(context).build();

        Token storedToken = tokenManager.getToken();
        assertNull(storedToken);
    }

    @Test
    public void persistTokenWithRetry() {
        final boolean[] flag = {true};
        tokenManager = DefaultSingleSignOnManager.builder()
                .encryptor(new Encryptor() {
                    @Override
                    public byte[] encrypt(byte[] clearText) {
                        if (flag[0]) {
                            flag[0] = false;
                            throw new RuntimeException();
                        } else {
                            return new byte[0];
                        }
                    }

                    @Override
                    public byte[] decrypt(byte[] encryptedData) {
                        return encryptedData;
                    }

                    @Override
                    public void reset() throws GeneralSecurityException, IOException {

                    }
                })
                .context(context).build();

        SSOToken token = new SSOToken("MyTokenValue");
        tokenManager.persist(token);
        assertNotNull(tokenManager.getToken());

    }
}
