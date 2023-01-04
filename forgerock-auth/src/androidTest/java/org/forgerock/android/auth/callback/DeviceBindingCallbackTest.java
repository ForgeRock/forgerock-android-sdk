/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.assertThat;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.nimbusds.jwt.JWTParser;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.DummyActivity;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.NodeListenerFuture;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.ParseException;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class DeviceBindingCallbackTest extends BaseDeviceBindingTest {
    protected final static String TREE = "device-bind";

    @Test
    public void testDeviceBindingDefaults() throws ExecutionException, InterruptedException {
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "default")
        {
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    NodeListener<FRSession> nodeListener = this;
                    Assert.assertNotNull(callback.getUserId());
                    assertThat(callback.getUserName()).isEqualTo(USERNAME);
                    assertThat(callback.getDeviceBindingAuthenticationType()).isEqualTo(DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK);
                    Assert.assertNotNull(callback.getChallenge());
                    assertThat(callback.getTitle()).isEqualTo("Authentication required");
                    assertThat(callback.getSubtitle()).isEqualTo("Cryptography device binding");
                    assertThat(callback.getDescription()).isEqualTo("Please complete with biometric to proceed");
                    assertThat(callback.getTimeout()).isEqualTo(60);

                    // Set "Abort" outcome, so that the journey finishes...
                    callback.setClientError("Abort");
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
    public void testDeviceBindingCustom() throws ExecutionException, InterruptedException {
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "custom")
        {
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    NodeListener<FRSession> nodeListener = this;
                    Assert.assertNotNull(callback.getUserId());
                    assertThat(callback.getUserName()).isEqualTo(USERNAME);
                    assertThat(callback.getDeviceBindingAuthenticationType()).isEqualTo(DeviceBindingAuthenticationType.NONE);
                    Assert.assertNotNull(callback.getChallenge());
                    assertThat(callback.getTitle()).isEqualTo("Custom title");
                    assertThat(callback.getSubtitle()).isEqualTo("Custom subtitle");
                    assertThat(callback.getDescription()).isEqualTo("Custom description");
                    assertThat(callback.getTimeout()).isEqualTo(5);

                    // Set "Abort" (without binding), so that the journey finishes...
                    callback.setClientError("Abort");
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
    public void testDeviceBindingBind() throws ExecutionException, InterruptedException {
        final int[] hit = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "custom")
        {
            NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    callback.bind(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            node.next(context, nodeListener);
                            hit[0]++;
                        }

                        @Override
                        public void onException(Exception e) {
                            Assertions.fail(e.getMessage());
                        }
                    });
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(hit[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testDeviceBindingExceed() throws ExecutionException, InterruptedException {
        final int[] hit = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "exceed-limit")
        {
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);
                    Assertions.fail("Device bind node did NOT trigger the expected 'Exceeded Device Limit' outcome");
                    NodeListener<FRSession> nodeListener = this;
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Device Limit Exceeded");
                    hit[0]++;

                    NodeListener<FRSession> nodeListener = this;
                    node.next(context, nodeListener);
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(hit[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testDeviceBindingCustomOutcome() throws ExecutionException, InterruptedException {
        final int[] hit = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "custom")
        {
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    // Set "Custom" client error
                    NodeListener<FRSession> nodeListener = this;
                    callback.setClientError("Custom");
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Custom outcome triggered");
                    hit[0]++;

                    NodeListener<FRSession> nodeListener = this;
                    node.next(context, nodeListener);
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(hit[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testDeviceBindingApplicationPin() throws ExecutionException, InterruptedException {
        final int[] bindSuccess = {0};
        CallbackFactory.getInstance().register(CustomApplicationPinDeviceBindingCallback.class);

        ActivityScenario scenario = ActivityScenario.launch(DummyActivity.class);
        scenario.onActivity(InitProvider::setCurrentActivity);

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "pin")
        {
            NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(CustomApplicationPinDeviceBindingCallback.class) != null) {
                    CustomApplicationPinDeviceBindingCallback callback = node.getCallback(CustomApplicationPinDeviceBindingCallback.class);
                    callback.getDeviceAuthenticator().pin = "1234";

                    callback.bind(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bindSuccess[0]++;
                            try {
                                // Get the kid
                                KID = JWTParser.parse((String) callback.getInputValue(0)).getHeader().toJSONObject().get("kid").toString();
                                Logger.debug(TAG, KID);
                            } catch (ParseException e) {
                                Assertions.fail(e.getMessage());
                            }
                            node.next(context, nodeListener);
                        }

                        @Override
                        public void onException(Exception e) {
                            Assertions.fail(e.getMessage());
                        }
                    });
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(bindSuccess[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }
}



