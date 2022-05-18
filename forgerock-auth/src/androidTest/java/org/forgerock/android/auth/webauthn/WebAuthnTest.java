/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.forgerock.android.auth.DummyActivity;
import org.forgerock.android.auth.MockServer;
import org.forgerock.android.auth.callback.CallbackFactory;
import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback;
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public abstract class WebAuthnTest extends MockServer {

    @Rule
    public Timeout timeout = new Timeout(10000, TimeUnit.MILLISECONDS);

    @Rule
    public ActivityTestRule<DummyActivity> activityRule = new ActivityTestRule<>(DummyActivity.class);

    @Before
    public void webAuthnSetUp() {
        //reset to the original callback
        CallbackFactory.getInstance().register(WebAuthnRegistrationCallback.class);
        CallbackFactory.getInstance().register(WebAuthnAuthenticationCallback.class);
    }

    @After
    public void tearDown() throws Exception {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("org.forgerock.test");
        for (Account acc : accounts) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager.removeAccountExplicitly(acc);
            } else {
                AccountManagerFuture<Boolean> future = accountManager.removeAccount(acc, null, null);
                future.getResult();
            }
        }
    }
}
