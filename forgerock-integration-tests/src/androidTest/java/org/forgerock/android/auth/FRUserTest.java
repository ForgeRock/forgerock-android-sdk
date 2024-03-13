/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class FRUserTest extends AndroidBaseTest {

    @After
    public void logoutUser() throws Exception {
        if (FRUser.getCurrentUser() != null) {
            FRUser.getCurrentUser().logout();
        }
    }

    @Test
    public void testLogin() throws ExecutionException, InterruptedException {
        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName(USERNAME);
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword(PASSWORD.toCharArray());
                    state.next(context, this);
                }
            }
        };

        FRUser.login(context, nodeListenerFuture);

        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        FRListenerFuture<UserInfo> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getUserInfo(future);
        UserInfo userinfo = future.get();
        assertEquals(USER_EMAIL, userinfo.getEmail());
    }

    @Test
    public void testAccessToken() throws Exception {
        testLogin();
        AccessToken accessToken = FRUser.getCurrentUser().getAccessToken();
        assertNotNull(accessToken.getValue());
    }
    @Test
    public void testAccessTokenAsync() throws Exception {
        testLogin();
        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getAccessToken(future);
        assertNotNull(future.get());
    }
    @Test
    public void testRefreshTokenAsync() throws Exception {
        testLogin();
        FRListenerFuture<AccessToken> future = new FRListenerFuture<>();
        FRUser.getCurrentUser().getAccessToken(future);
        AccessToken token = future.get();
        assertNotNull(token);
        FRListenerFuture<AccessToken> refreshTokenFuture = new FRListenerFuture<>();
        FRUser.getCurrentUser().refresh(refreshTokenFuture);
        AccessToken token1 = future.get();
        assertNotNull(token1);
    }

    @Test
    public void testLogout() throws InterruptedException, ExecutionException {
        testLogin();
        FRUser.getCurrentUser().logout();
        assertNull(FRUser.getCurrentUser());
    }

}
