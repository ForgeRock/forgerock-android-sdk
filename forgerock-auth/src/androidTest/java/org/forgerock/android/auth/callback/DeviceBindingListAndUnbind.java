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
import org.forgerock.android.auth.devicebind.UserKey;
import org.forgerock.android.auth.exception.ApiException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DeviceBindingListAndUnbind extends BaseDeviceBindingTest {
    protected final static String TREE = "device-verifier";
    protected final static String APPLICATION_PIN = "1234";

    public void bindDevice(String nodeConfiguration) throws ExecutionException, InterruptedException {
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

    // Login using DeviceVerifier node
    public void loginWithKey() throws ExecutionException, InterruptedException {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
        final int[] loginSuccess = {0};

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);

                    callback.sign(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            node.next(context, nodeListener);
                        }

                        @Override
                        public void onException(Exception e) {
                            // Signing of the challenge has failed unexpectedly...
                            Assertions.fail(e.getMessage());
                        }
                    });

                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Success");
                    loginSuccess[0]++;

                    node.next(context, nodeListener);
                    return;
                }

                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testListAndDeleteKeys() throws ExecutionException, InterruptedException, IOException, ApiException {
        bindDevice("bind-pin"); // Bind the device with APPLICATION_PIN
        bindDevice("bind");     // Bind the device with NONE

        final int[] deletionFailure = {0};
        final int[] deletionSuccess = {0};
        FRUserKeys userKeys = new FRUserKeys(context);
        //Make sure we only keep one key per user.
        assertThat(userKeys.loadAll().size()).isEqualTo(1);

        // Assert that the keys are indeed associated with the correct user
        assertThat(userKeys.loadAll().get(0).getUserName()).isEqualTo(USERNAME);

        // Attempt to remove a key without being authenticated with it should fail
        FRListenerFuture<Void> future = new FRListenerFuture<Void>();
        userKeys.delete(userKeys.loadAll().get(0), false, future);
        try {
            future.get();
            Assertions.fail("Attempt to remove a device key without being authenticated with it should fail!");
        }
        catch (Exception e) {
            assertThat(e.getCause().getMessage()).isEqualTo("Failed to delete resources");
            deletionFailure[0]++;
        }
        assertThat(deletionSuccess[0]).isEqualTo(0);
        assertThat(deletionFailure[0]).isEqualTo(1);

        // Make sure that none of the local keys were removed (should be still 2, since we didn't force local deletion)
        assertThat(1).isEqualTo(userKeys.loadAll().size());

        // Authenticate using the "NONE" authentication key
        loginWithKey();
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());

        // Removing the "NONE" authentication key should work now
        UserKey userKeyNone = null;

        List<UserKey> userKeysList = userKeys.loadAll();
        for (int i = 0; i < userKeysList.size(); i++) {
            if(userKeysList.get(i).getAuthType().name().equals("NONE")) {
                userKeyNone = userKeysList.get(i);
                break;
            }
        }

        Assert.assertNotNull(userKeyNone);

        future = new FRListenerFuture<Void>();
        userKeys.delete(userKeyNone,false, future);
        try {
            future.get();
            deletionSuccess[0]++;
        }
        catch (Exception e) {
            deletionFailure[0]++;
        }

        assertThat(deletionSuccess[0]).isEqualTo(1);
        assertThat(deletionFailure[0]).isEqualTo(1);
        assertThat(0).isEqualTo(userKeys.loadAll().size());

        // Logout...
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
    }

    @Test
    public void testForceDelete() throws ExecutionException, InterruptedException {
        bindDevice("bind");     // Bind the device with NONE
        final int[] deletionFailure = {0};
        final int[] deletionSuccess = {0};
        FRUserKeys userKeys = new FRUserKeys(context);
        assertThat(userKeys.loadAll().size()).isEqualTo(1);

        // Assert that the keys are indeed associated with the correct user
        assertThat(userKeys.loadAll().get(0).getUserName()).isEqualTo(USERNAME);

        // Attempt to remove a key without being authenticated with it should fail
        FRListenerFuture<Void> future = new FRListenerFuture<Void>();
        userKeys.delete(userKeys.loadAll().get(0), false, future);
        try {
            future.get();
            Assertions.fail("Attempt to remove a device key without being authenticated with it should fail!");
        }
        catch (Exception e) {
            assertThat(e.getCause().getMessage()).isEqualTo("Failed to delete resources");
            deletionFailure[0]++;
        }
        assertThat(deletionSuccess[0]).isEqualTo(0);
        assertThat(deletionFailure[0]).isEqualTo(1);

        // Make sure that none of the local keys were removed (should be still 1, since we didn't force local deletion)
        assertThat(1).isEqualTo(userKeys.loadAll().size());


        // Delete the second key with "force" and confirm it was deleted locally
        // Note that the remote deletion should still return an exception...
        future = new FRListenerFuture<>();
        userKeys.delete(userKeys.loadAll().get(0), true, future);
        try {
            future.get();
            Assertions.fail("Attempt to remove a device key without being authenticated with it should fail!");
        }
        catch (Exception e) {
            assertThat(e.getCause().getMessage()).isEqualTo("Failed to delete resources");
            deletionFailure[0]++;
        }
        assertThat(deletionSuccess[0]).isEqualTo(0);
        assertThat(deletionFailure[0]).isEqualTo(2);

        // Make sure that even though we got an error from server, the local key was removed!
        assertThat(0).isEqualTo(userKeys.loadAll().size());

    }
}