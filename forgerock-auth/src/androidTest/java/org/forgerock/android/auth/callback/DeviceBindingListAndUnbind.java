/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.assertThat;

import androidx.test.core.app.ActivityScenario;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.DummyActivity;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRListenerFuture;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.FRUserKeys;
import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.NodeListenerFuture;
import org.forgerock.android.auth.devicebind.ApplicationPinDeviceAuthenticator;
import org.forgerock.android.auth.exception.ApiException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class DeviceBindingListAndUnbind extends BaseDeviceBindingTest {
    protected final static String TREE = "device-verifier";
    protected final static String APPLICATION_PIN = "1234";

    @BeforeClass
    public static void bindDevices() throws ExecutionException, InterruptedException {
        bindDevice("bind-pin"); // Bind the device with APPLICATION_PIN
        bindDevice("bind");     // Bind the device with NONE
    }

    public static void bindDevice(String nodeConfiguration) throws ExecutionException, InterruptedException {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
        final int[] bindSuccess = {0};

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, nodeConfiguration) {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    USER_ID = callback.getUserId();

                    // Bind device with PIN...
                    if("bind-pin".equals(nodeConfiguration)) {
                        ActivityScenario<DummyActivity> scenario = ActivityScenario.launch(DummyActivity.class);
                        scenario.onActivity(InitProvider::setCurrentActivity);

                        callback.bind(context, deviceBindingAuthenticationType ->
                                    new ApplicationPinDeviceAuthenticator((prompt, fragmentActivity, $completion) -> APPLICATION_PIN.toCharArray()),
                            new FRListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    bindSuccess[0]++;
                                    node.next(context, nodeListener);
                                }

                                @Override
                                public void onException(Exception e) {
                                    Assertions.fail(e.getMessage());
                                }
                            });
                    }
                    else {
                        callback.bind(context,
                            new FRListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    bindSuccess[0]++;
                                    node.next(context, nodeListener);
                                }

                                @Override
                                public void onException(Exception e) {
                                    Assertions.fail(e.getMessage());
                                }
                            });
                    }
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(bindSuccess[0]).isEqualTo(1);

        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());

        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
    }

    @Test
    public void testListAndDeleteKeys() throws ExecutionException, InterruptedException, IOException, ApiException {
        FRUserKeys userKeys = new FRUserKeys(context);
        assertThat(userKeys.loadAll().size()).isEqualTo(2);

        // Assert that the keys are indeed associated with the correct user
        assertThat(userKeys.loadAll().get(0).getUserName()).isEqualTo(USERNAME);
        assertThat(userKeys.loadAll().get(1).getUserName()).isEqualTo(USERNAME);

        // Delete one of the keys and confirm it was deleted
        FRListenerFuture<Void> future = new FRListenerFuture<>();
        userKeys.delete(userKeys.loadAll().get(0), false, future);
        future.get();

        assertThat(1).isEqualTo(userKeys.loadAll().size());

        // Delete the second key and confirm it was deleted
        FRListenerFuture<Void> future2 = new FRListenerFuture<>();
        userKeys.delete(userKeys.loadAll().get(0), false, future2);
        future2.get();

        assertThat(0).isEqualTo(userKeys.loadAll().size());
    }
}