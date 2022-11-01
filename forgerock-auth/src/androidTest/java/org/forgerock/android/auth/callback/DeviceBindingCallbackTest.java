/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.DeviceBindingNodeListener;
import org.forgerock.android.auth.FRAuth;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FROptions;
import org.forgerock.android.auth.FROptionsBuilder;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.NodeListenerFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.Timeout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import static org.assertj.core.api.Assertions.assertThat;
import android.content.Context;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class DeviceBindingCallbackTest {
    protected Context context = ApplicationProvider.getApplicationContext();

    // This test uses dynamic configuration with the following settings:
    protected final String AM_URL = "https://openam-dbind.forgeblocks.com/am";
    protected final String REALM = "alpha";
    protected final String OAUTH_CLIENT = "AndroidTest";
    protected final String OAUTH_REDIRECT_URI = "org.forgerock.demo:/oauth2redirect";
    protected final String SCOPE = "openid profile email address phone";
    protected final String TREE = "device-bind";

    protected static String USERNAME = "sdkuser";
    protected static String PASSWORD = "password";
    protected static String USER_EMAIL = "sdkuser@example.com";

    @Rule
    public Timeout timeout = new Timeout(10000, TimeUnit.MILLISECONDS);

    @Before
    public void setUpSDK() {
        Logger.set(Logger.Level.DEBUG);

        FROptions options = FROptionsBuilder.build(builder -> {
            builder.server(serverBuilder -> {
                 serverBuilder.setUrl(AM_URL);
                 serverBuilder.setRealm(REALM);
                 return null;
             });
             builder.service(service-> {
                 service.setAuthServiceName(TREE);
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
    }

    @After
    public void logoutSession() {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
    }

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
                    // assertThat(callback.getUserName()).isEqualTo(USERNAME);
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
                    // assertThat(callback.getUserName()).isEqualTo(USERNAME);
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
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    NodeListener<FRSession> nodeListener = this;

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
}



