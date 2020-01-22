/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidVersionAwareTestRunner.class)
public class DefaultSingleSignOnManagerTest {

    private SingleSignOnManager tokenManager;
    private Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        tokenManager = DefaultSingleSignOnManager.builder()
                .context(context).build();
    }

    @After
    public void cleanup() throws Exception {
        tokenManager.clear();
        new AsymmetricEncryptor(context, "org.forgerock.v1.SSO_TOKEN").reset();
    }

    @Test
    public void storeToken() {
        SSOToken token = new SSOToken("MyTokenValue");

        tokenManager.persist(token);

        Token storedToken = tokenManager.getToken();
        assertEquals("MyTokenValue", storedToken.getValue());
    }

    @Test
    public void clearAccessToken() {
        SSOToken token = new SSOToken("MyTokenValue");
        tokenManager.persist(token);
        tokenManager.clear();

        Token storedToken = tokenManager.getToken();
        assertNull(storedToken);
    }

    @Test
    @TargetApi(23)
    public void testUpgrade() {
        SecretKeyStore secretKeyStore = new SharedPreferencesSecretKeyStore("Test", context.getSharedPreferences("test", Context.MODE_PRIVATE));
        tokenManager = DefaultSingleSignOnManager.builder()
                .encryptor(new AndroidLEncryptor(context, "org.forgerock.v1.SSO_KEYS", secretKeyStore))
                .context(context).build();

        SSOToken token = new SSOToken("MyTokenValue");
        tokenManager.persist(token);
        Token storedToken = tokenManager.getToken();
        assertEquals("MyTokenValue", storedToken.getValue());

        //upgrade now
        tokenManager = DefaultSingleSignOnManager.builder()
                .encryptor(new AndroidMEncryptor("org.forgerock.v1.SSO_KEYS", null))
                .context(context).build();

        storedToken = tokenManager.getToken();
        assertNull(storedToken);

        token = new SSOToken("MyTokenValue2");

        tokenManager.persist(token);

        storedToken = tokenManager.getToken();
        assertEquals("MyTokenValue2", storedToken.getValue());

    }
}
