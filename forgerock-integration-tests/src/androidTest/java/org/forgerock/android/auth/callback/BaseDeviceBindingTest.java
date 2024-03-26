/*
 * Copyright (c) 2023 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.android.auth.devicebind.LocalDeviceBindingRepositoryKt.ORG_FORGEROCK_V_1_DEVICE_REPO;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.forgerock.android.auth.AndroidBaseTest;
import org.forgerock.android.auth.EncryptedPreferences;
import org.forgerock.android.auth.FRAuth;
import org.forgerock.android.auth.FROptions;
import org.forgerock.android.auth.FROptionsBuilder;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.RetryTestRule;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class BaseDeviceBindingTest {
    protected static Context context = ApplicationProvider.getApplicationContext();

    // This test uses dynamic configuration with the following settings:
    protected final static String AM_URL = "https://openam-dbind.forgeblocks.com/am";
    protected final static String REALM = "alpha";
    protected final static String OAUTH_CLIENT = "AndroidTest";
    protected final static String OAUTH_REDIRECT_URI = "org.forgerock.demo:/oauth2redirect";
    protected final static String SCOPE = "openid profile email address phone";

    protected static String USERNAME = null;
    protected static String KID = null; // Used to store the kid of the key generated during binding
    protected static String USER_ID = null; // Used to store the userId of the user who binds the device

    @Rule
    public Timeout timeout = new Timeout(20000, TimeUnit.MILLISECONDS);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUpSDK() {
        Logger.set(Logger.Level.DEBUG);

        // Prepare dynamic configuration object
        FROptions options = FROptionsBuilder.build(builder -> {
            builder.server(serverBuilder -> {
                serverBuilder.setUrl(AM_URL);
                serverBuilder.setRealm(REALM);
                return null;
            });
            builder.oauth(oauth -> {
                oauth.setOauthClientId(OAUTH_CLIENT);
                oauth.setOauthRedirectUri(OAUTH_REDIRECT_URI);
                oauth.setOauthScope(SCOPE);
                return null;
            });
            return null;
        });

        FRAuth.start(context, options);
        // Clear all preexisting registered keys on the device
        EncryptedPreferences.Companion.getInstance(context, ORG_FORGEROCK_V_1_DEVICE_REPO).edit().clear().apply();

        // Register a random user. This is needed to avoid test failure when tests are run in parallel on multiple devices
        try {
            USERNAME = AndroidBaseTest.registerRandomUser();
        } catch (ExecutionException e) {
            fail("Failed to register a new random user: ", e);
        } catch (InterruptedException e) {
            fail("Failed to register a new random user: ", e);
        }
    }

    @After
    public void logoutSession() {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
        CallbackFactory.getInstance().register(DeviceBindingCallback.class);
        CallbackFactory.getInstance().register(DeviceSigningVerifierCallback.class);
    }
}
