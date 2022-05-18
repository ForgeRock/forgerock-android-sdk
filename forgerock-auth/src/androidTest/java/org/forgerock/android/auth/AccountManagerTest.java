/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.accounts.*;
import android.content.Context;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class AccountManagerTest extends AndroidBaseTest {

    @Test
    @Ignore("For AccountManager interface testing, not SDK function")
    public void name() throws AuthenticatorException, OperationCanceledException, IOException {
        Account account = new Account("testuser", "org.forgerock");
        AccountManager am = AccountManager.get(context);
        boolean result = am.addAccountExplicitly(account, null, null);
        am.setAuthToken(account, "SSO", "12345");
        AccountManagerFuture<Bundle> future = am.getAuthToken(account, "SSO", null, null, null, null);
        Bundle bundle = future.getResult();
        Assert.assertEquals("12345", bundle.getString(AccountManager.KEY_AUTHTOKEN));
        am.setUserData(account, "name", "value");
        Assert.assertEquals("value", am.getUserData(account, "name"));
        am.removeAccountExplicitly(account);
        System.out.println(result);
    }
}
