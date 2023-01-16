/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.assertThat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;

import com.nimbusds.jwt.JWT;
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
import org.forgerock.android.auth.devicebind.ApplicationPinDeviceAuthenticator;
import org.forgerock.android.auth.devicebind.DefaultUserKeySelector;
import org.forgerock.android.auth.devicebind.DeviceAuthenticator;
import org.forgerock.android.auth.devicebind.PinCollector;
import org.forgerock.android.auth.devicebind.Prompt;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function1;

public class DeviceSigningVerifierApplicationPinCallbackTest extends BaseDeviceBindingTest {
    protected final static String TREE = "device-verifier";
    protected final static String APPLICATION_PIN = "1234";

    @BeforeClass
    public static void bindDevice() throws ExecutionException, InterruptedException {
        final int[] bindSuccess = {0};

        ActivityScenario<DummyActivity> scenario = ActivityScenario.launch(DummyActivity.class);
        scenario.onActivity(InitProvider::setCurrentActivity);

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "bind-pin") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    USER_ID = callback.getUserId();

                    // Bind the device...
                    callback.bind(context, deviceBindingAuthenticationType ->
                                    new ApplicationPinDeviceAuthenticator((prompt, fragmentActivity, $completion) -> APPLICATION_PIN.toCharArray()),
                            new FRListener<Void>() {
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

        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());

        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
    }

    @Test
    public void testDeviceVerificationWithCorrectApplicationPin() throws ExecutionException, InterruptedException {
        final int[] signSuccess = {0};
        final int[] authSuccess = {0};
        ActivityScenario<DummyActivity> scenario = ActivityScenario.launch(DummyActivity.class);
        scenario.onActivity(InitProvider::setCurrentActivity);

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);

                    Assert.assertNotNull(callback.getUserId());
                    Assert.assertNotNull(callback.getChallenge());

                    callback.sign(context, new DefaultUserKeySelector(),
                            deviceBindingAuthenticationType -> new ApplicationPinDeviceAuthenticator((prompt, fragmentActivity, $completion) -> APPLICATION_PIN.toCharArray()),
                            new FRListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    // Verify the JWT attributes
                                    try {
                                        Calendar expMin = Calendar.getInstance();
                                        Calendar expMax = Calendar.getInstance();
                                        expMin.add(Calendar.SECOND, 55);
                                        expMax.add(Calendar.SECOND, 60);

                                        JWT jwt = JWTParser.parse((String) callback.getInputValue(0));
                                        String jwtKid = jwt.getHeader().toJSONObject().get("kid").toString();
                                        Date jwtExp = jwt.getJWTClaimsSet().getExpirationTime();
                                        String jwtChallenge = jwt.getJWTClaimsSet().getStringClaim("challenge");
                                        String jwtSub = jwt.getJWTClaimsSet().getSubject();

                                        assertThat(jwtKid).isEqualTo(KID);
                                        assertThat(jwtExp).isBetween(expMin.getTime(), expMax.getTime());
                                        assertThat(jwtChallenge).isEqualTo(callback.getChallenge());
                                        assertThat(jwtSub).isEqualTo(callback.getUserId());

                                        signSuccess[0]++;
                                        node.next(context, nodeListener);
                                    } catch (ParseException e) {
                                        Assertions.fail("Invalid JWT: " + e.getMessage());
                                    }
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
                    authSuccess[0]++;

                    node.next(context, nodeListener);
                    return;
                }

                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(signSuccess[0]).isEqualTo(1);
        assertThat(authSuccess[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testDeviceVerificationWithWrongApplicationPin() throws ExecutionException, InterruptedException {
        final int[] signFailure = {0};

        ActivityScenario<DummyActivity> scenario = ActivityScenario.launch(DummyActivity.class);
        scenario.onActivity(InitProvider::setCurrentActivity);

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);

                    Assert.assertNotNull(callback.getUserId());
                    Assert.assertNotNull(callback.getChallenge());

                    callback.sign(context, new DefaultUserKeySelector(),
                            deviceBindingAuthenticationType -> new ApplicationPinDeviceAuthenticator((prompt, fragmentActivity, $completion) -> "WRONG".toCharArray()),
                            new FRListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    node.next(context, nodeListener);
                                }

                                @Override
                                public void onException(Exception e) {
                                    assertThat(e.getMessage()).isEqualTo("Invalid Credentials");
                                    signFailure[0]++;
                                    node.next(context, nodeListener);
                                }
                            });
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Unsupported");

                    node.next(context, nodeListener);
                    return;
                }

                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        // Ensure that the journey finishes with failure
        thrown.expect(java.util.concurrent.ExecutionException.class);
        thrown.expectMessage("org.forgerock.android.auth.exception.AuthenticationException: {\"code\":401,\"reason\":\"Unauthorized\",\"message\":\"Login failure\"}");

        Assert.assertNull(nodeListenerFuture.get());
        Assert.assertNull(FRSession.getCurrentSession());
        Assert.assertNull(FRSession.getCurrentSession().getSessionToken());
        assertThat(signFailure[0]).isEqualTo(1);
    }

    @Test
    public void testDeviceVerificationUsernamelessApplicationPin() throws ExecutionException, InterruptedException {
        final int[] signSuccess = {0};
        final int[] authSuccess = {0};

        ActivityScenario<DummyActivity> scenario = ActivityScenario.launch(DummyActivity.class);
        scenario.onActivity(InitProvider::setCurrentActivity);

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "usernameless") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);

                    // In usernameless userId in the callback is empty...
                    assertThat(callback.getUserId()).isEmpty();
                    Assert.assertNotNull(callback.getChallenge());

                    callback.sign(context, new DefaultUserKeySelector(),
                            deviceBindingAuthenticationType -> new ApplicationPinDeviceAuthenticator((prompt, fragmentActivity, $completion) -> APPLICATION_PIN.toCharArray()),
                            new FRListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    // Verify the JWT attributes
                                    try {
                                        Calendar expMin = Calendar.getInstance();
                                        Calendar expMax = Calendar.getInstance();
                                        expMin.add(Calendar.SECOND, 55);
                                        expMax.add(Calendar.SECOND, 60);

                                        JWT jwt = JWTParser.parse((String) callback.getInputValue(0));
                                        String jwtKid = jwt.getHeader().toJSONObject().get("kid").toString();
                                        Date jwtExp = jwt.getJWTClaimsSet().getExpirationTime();
                                        String jwtChallenge = jwt.getJWTClaimsSet().getStringClaim("challenge");
                                        String jwtSub = jwt.getJWTClaimsSet().getSubject();

                                        assertThat(jwtKid).isEqualTo(KID);
                                        assertThat(jwtExp).isBetween(expMin.getTime(), expMax.getTime());
                                        assertThat(jwtChallenge).isEqualTo(callback.getChallenge());
                                        assertThat(jwtSub).isEqualTo(USER_ID);

                                        signSuccess[0]++;
                                        node.next(context, nodeListener);
                                    } catch (ParseException e) {
                                        Assertions.fail("Invalid JWT: " + e.getMessage());
                                    }
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
                    authSuccess[0]++;

                    node.next(context, nodeListener);
                    return;
                }

                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(signSuccess[0]).isEqualTo(1);
        assertThat(authSuccess[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }
}