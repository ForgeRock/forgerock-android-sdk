/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public abstract class TreeTest extends AndroidBaseTest {

    protected abstract String getTreeName();

    protected abstract NodeListenerFuture<FRSession> getNodeListenerFuture();

    @After
    public void logoutSession() {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
    }

    @Test
    public void testTree() throws ExecutionException, InterruptedException {

        NodeListenerFuture<FRSession> nodeListenerFuture = getNodeListenerFuture();

        FRSession.authenticate(context, getTreeName(), nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());

    }

}
