/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;

public abstract class AndroidBaseTest {

    protected Context context = ApplicationProvider.getApplicationContext();
    public static String USERNAME = "sdkuser";
    public static String PASSWORD = "password";
    public static String USER_EMAIL = "sdkuser@example.com";

    protected String TREE = "UsernamePassword";

    @Before
    public void setUpSDK() {
        Logger.set(Logger.Level.DEBUG);
        FRAuth.start(context);
    }
}
