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
        new AsymmetricEncryptor(context, DefaultSingleSignOnManager.ORG_FORGEROCK_V_1_SSO_KEYS).reset();
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
        SingleSignOnManager androidLSSOManager = new AndroidLSingleSignOnManager(context, null);
        SSOToken token = new SSOToken("MyTokenValue");
        androidLSSOManager.persist(token);
        Token storedToken = androidLSSOManager.getToken();
        assertEquals("MyTokenValue", storedToken.getValue());

        //upgrade now
        SingleSignOnManager androidMSSOManager = new AndroidMSingleSignOnManager(context, null);
        storedToken = androidMSSOManager.getToken();
        assertNull(storedToken);

        token = new SSOToken("MyTokenValue2");

        androidMSSOManager.persist(token);

        storedToken = androidMSSOManager.getToken();
        assertEquals("MyTokenValue2", storedToken.getValue());

    }
}
