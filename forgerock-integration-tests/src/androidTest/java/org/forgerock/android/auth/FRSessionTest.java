/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.android.auth.Action.AUTHENTICATE;

@RunWith(AndroidJUnit4.class)
public class FRSessionTest extends AndroidBaseTest {

    @After
    public void logoutSession() throws Exception {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
    }

    @Test
    public void frSessionHappyPath() throws ExecutionException, InterruptedException {

        UsernamePasswordNodeListener nodeListenerFuture = new UsernamePasswordNodeListener(context);
        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());

    }

    @Test
    public void testWithNoSession() throws ExecutionException, InterruptedException {

        RequestInterceptorRegistry.getInstance().register((FRRequestInterceptor<Action>) (request, tag) -> {
            if (tag.getType().equals(AUTHENTICATE)) {
                return request.newBuilder()
                        .url(Uri.parse(request.url().toString())
                                .buildUpon()
                                .appendQueryParameter("noSession", "true").toString())
                        .build();
            }
            return request;
        });

        UsernamePasswordNodeListener nodeListenerFuture = new UsernamePasswordNodeListener(context);
        FRSession.authenticate(context, TREE, nodeListenerFuture);
        assertThat(nodeListenerFuture.get()).isNull();
        assertThat(FRSession.getCurrentSession()).isNull();
        assertThat(FRUser.getCurrentUser()).isNull();

        RequestInterceptorRegistry.getInstance().register(null);

    }

    @Test
    public void testLogout() throws ExecutionException, InterruptedException {

        frSessionHappyPath();

        FRSession.getCurrentSession().logout();

        //Check SSOToken Storage
        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .context(context)
                .build();

        assertThat(singleSignOnManager.getToken()).isNull();
        assertThat(FRSession.getCurrentSession()).isNull();


    }
}
