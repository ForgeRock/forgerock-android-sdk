/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidVersionAwareTestRunner.class)
public class DefaultSingleSignOnManagerTest extends AndroidBaseTest {

    private SingleSignOnManager tokenManager;

    @Before
    public void setUp() {
        tokenManager = DefaultSingleSignOnManager.builder()
                .context(context).build();
    }

    @After
    public void cleanup() throws Exception {
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
    public void clearSSOToken() {
        SSOToken token = new SSOToken("MyTokenValue");
        tokenManager.persist(token);
        tokenManager.clear();

        Token storedToken = tokenManager.getToken();
        assertNull(storedToken);
    }


}
