/*
 * Copyright (c) 2019 - 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidVersionAwareTestRunner.class)
public class DefaultSingleSignOnManagerTest extends AndroidBaseTest {

    public static final String ORG_FORGEROCK = "org.forgerock.test";
    private SingleSignOnManager tokenManager;

    @Before
    public void setUp() {
        tokenManager = DefaultSingleSignOnManager.builder()
                .context(context).build();
    }

    @After
    public void cleanup() throws Exception {
        new AsymmetricEncryptor(context, "org.forgerock.v1.SSO_TOKEN").reset();

        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(ORG_FORGEROCK);
        for (Account acc : accounts) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager.removeAccountExplicitly(acc);
            } else {
                AccountManagerFuture<Boolean> future = accountManager.removeAccount(acc, null, null);
                future.getResult();
            }
        }
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

    @Test
    public void testAccount() {
        SSOToken token = new SSOToken("MyTokenValue");
        tokenManager.persist(token);

        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(ORG_FORGEROCK);
        //Only one account Created
        Assertions.assertThat(accounts).hasSize(1);
        Assertions.assertThat(accounts[0].name).isEqualTo("ForgeRock");

        tokenManager.clear();
        accounts = accountManager.getAccountsByType(ORG_FORGEROCK);
        Assertions.assertThat(accounts).hasSize(0);

    }

    @Test
    public void testAccountNotCreatedBySDK() throws AuthenticatorException, OperationCanceledException, IOException {
        SSOToken token = new SSOToken("MyTokenValue");
        tokenManager.persist(token);

        AccountManager accountManager = AccountManager.get(context);
        Account account = new Account("Dummy", ORG_FORGEROCK);
        accountManager.addAccountExplicitly(account, null, null);
        Account[] accounts = accountManager.getAccountsByType(ORG_FORGEROCK);
        Assertions.assertThat(accounts).hasSize(2);

        //Assert that TokenManager has successfully remove the account
        tokenManager.clear();
        accounts = accountManager.getAccountsByType(ORG_FORGEROCK);
        Assertions.assertThat(accounts).hasSize(1);
        Assertions.assertThat(accounts[0].name).isEqualTo("Dummy");
        //cleanup
        AccountManagerFuture<Boolean> future = accountManager.removeAccount(accounts[0], null, null);
        future.getResult();

    }

    @Test
    public void testPersistEmptyData() {

        SSOToken ssoToken = new SSOToken("");
        tokenManager.persist(ssoToken);

        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(ORG_FORGEROCK);
        //Account should not be created
        Assertions.assertThat(accounts).hasSize(1);

    }



}
