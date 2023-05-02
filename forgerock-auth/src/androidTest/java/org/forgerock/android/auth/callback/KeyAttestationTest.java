/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.assertThat;

import android.os.Build;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.DummyActivity;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.InitProvider;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.NodeListenerFuture;
import org.forgerock.android.auth.devicebind.ApplicationPinDeviceAuthenticator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class KeyAttestationTest extends BaseDeviceBindingTest {
    protected final static String TREE = "key-attestation";

    @Test
    public void testKeyAttestationNoneNone() throws ExecutionException, InterruptedException {
        // Test that when "Key Attestation" is set to NONE in AM, the SDK does not include x5c (X.509 Certificate Chain) parameter in the JWK...
        final int[] bindSuccess = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "none-none") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    callback.bind(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bindSuccess[0]++;

                            // Validate the jwt sent to AM...
                            try {
                                JWT jwt = JWTParser.parse((String) callback.getInputValue(0));
                                String jwkAlg = ((SignedJWT) jwt).getHeader().getJWK().getAlgorithm().toString();
                                String jwkUse = ((SignedJWT) jwt).getHeader().getJWK().getKeyUse().toString();
                                List<Base64> x5c = ((SignedJWT) jwt).getHeader().getJWK().getX509CertChain();

                                assertThat(jwkAlg).isEqualTo("RS512");
                                assertThat(jwkUse).isEqualTo("sig");
                                assertThat(x5c).isNull(); // When Android Key Attestation property is set to NONE in AM

                                /// Assert some other properties
                                assertThat(jwt.getJWTClaimsSet().getClaim("iss")).isEqualTo("org.forgerock.android.auth.test");
                                assertThat(jwt.getJWTClaimsSet().getClaim("platform")).isEqualTo("android");
                                assertThat(jwt.getJWTClaimsSet().getClaim("android-version")).isEqualTo(Long.valueOf(Build.VERSION.SDK_INT));

                            } catch (ParseException e) {
                                throw new RuntimeException(e);
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
        assertThat(bindSuccess[0]).isEqualTo(1); /// Make sure that device binding was successful...

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testKeyAttestationNoneDefault() throws ExecutionException, InterruptedException {
        // Make sure that when "Key Attestation" is set to DEFAULT in AM, the SDK includes x5c (X.509 Certificate Chain) parameter...
        final int[] bindSuccess = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "none-default") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    callback.bind(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bindSuccess[0]++;

                            // Validate the jwt sent to AM...
                            try {
                                JWT jwt = JWTParser.parse((String) callback.getInputValue(0));
                                String jwkAlg = ((SignedJWT) jwt).getHeader().getJWK().getAlgorithm().toString();
                                String jwkUse = ((SignedJWT) jwt).getHeader().getJWK().getKeyUse().toString();
                                List<Base64> x5c = ((SignedJWT) jwt).getHeader().getJWK().getX509CertChain();

                                assertThat(jwkAlg).isEqualTo("RS512");
                                assertThat(jwkUse).isEqualTo("sig");
                                assertThat(x5c).isNotNull(); // When Android Key Attestation is set to DEFAULT in AM

                                /// Assert some other properties
                                assertThat(jwt.getJWTClaimsSet().getClaim("iss")).isEqualTo("org.forgerock.android.auth.test");
                                assertThat(jwt.getJWTClaimsSet().getClaim("platform")).isEqualTo("android");
                                assertThat(jwt.getJWTClaimsSet().getClaim("android-version")).isEqualTo(Long.valueOf(Build.VERSION.SDK_INT));
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
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
        assertThat(bindSuccess[0]).isEqualTo(1); /// Make sure that device binding was successful...

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }
    
    @Test
    public void testKeyAttestationNoneCustomPass() throws ExecutionException, InterruptedException {
        // Make sure that when "Key Attestation" is set to CUSTOM in AM, the SDK includes x5c (X.509 Certificate Chain) parameter...
        // Make sure that when the custom script returns "true", the device binding node outcome is success...
        final int[] bindSuccess = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "none-custom-pass") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    callback.bind(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bindSuccess[0]++;

                            // Validate the jwt sent to AM...
                            try {
                                JWT jwt = JWTParser.parse((String) callback.getInputValue(0));
                                String jwkAlg = ((SignedJWT) jwt).getHeader().getJWK().getAlgorithm().toString();
                                String jwkUse = ((SignedJWT) jwt).getHeader().getJWK().getKeyUse().toString();
                                List<Base64> x5c = ((SignedJWT) jwt).getHeader().getJWK().getX509CertChain();

                                assertThat(jwkAlg).isEqualTo("RS512");
                                assertThat(jwkUse).isEqualTo("sig");
                                assertThat(x5c).isNotNull(); // When Android Key Attestation is set to CUSTOM in AM

                                /// Assert some other properties
                                assertThat(jwt.getJWTClaimsSet().getClaim("iss")).isEqualTo("org.forgerock.android.auth.test");
                                assertThat(jwt.getJWTClaimsSet().getClaim("platform")).isEqualTo("android");
                                assertThat(jwt.getJWTClaimsSet().getClaim("android-version")).isEqualTo(Long.valueOf(Build.VERSION.SDK_INT));
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
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
        assertThat(bindSuccess[0]).isEqualTo(1); /// Make sure that device binding was successful...

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testKeyAttestationNoneCustomFail() {
        // Make sure that when "Key Attestation" is set to CUSTOM in AM, the SDK includes x5c (X.509 Certificate Chain) parameter...
        // Make sure that when the custom script returns "false", the device binding node outcome is failure...
        final int[] bindSuccess = {0};
        final int[] failureOutcome = {0};
        boolean executionExceptionOccurred = false;

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "none-custom-fail") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    callback.bind(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bindSuccess[0]++;

                            // Validate the jwt sent to AM...
                            try {
                                JWT jwt = JWTParser.parse((String) callback.getInputValue(0));
                                String jwkAlg = ((SignedJWT) jwt).getHeader().getJWK().getAlgorithm().toString();
                                String jwkUse = ((SignedJWT) jwt).getHeader().getJWK().getKeyUse().toString();
                                List<Base64> x5c = ((SignedJWT) jwt).getHeader().getJWK().getX509CertChain();

                                assertThat(jwkAlg).isEqualTo("RS512");
                                assertThat(jwkUse).isEqualTo("sig");
                                assertThat(x5c).isNotNull(); // When Android Key Attestation is set to CUSTOM in AM

                                /// Assert some other properties
                                assertThat(jwt.getJWTClaimsSet().getClaim("iss")).isEqualTo("org.forgerock.android.auth.test");
                                assertThat(jwt.getJWTClaimsSet().getClaim("platform")).isEqualTo("android");
                                assertThat(jwt.getJWTClaimsSet().getClaim("android-version")).isEqualTo(Long.valueOf(Build.VERSION.SDK_INT));
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            node.next(context, nodeListener);
                        }

                        @Override
                        public void onException(Exception e) {
                            Assert.fail("Unexpected failure.");
                            node.next(context, nodeListener);
                        }
                    });
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Failure");
                    failureOutcome[0]++;

                    node.next(context, nodeListener);
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        // Ensure that the journey finishes with failure
        try {
            Assert.assertNull(nodeListenerFuture.get());
        } catch (ExecutionException e) {
            executionExceptionOccurred = true;
            assertThat(e.getMessage()).isEqualTo("ApiException{statusCode=401, error='', description='{\"code\":401,\"reason\":\"Unauthorized\",\"message\":\"Login failure\"}'}");
        } catch (InterruptedException e) {
            Assert.fail("Unexpected exception.");
        }
        Assert.assertNull(FRSession.getCurrentSession());

        assertThat(bindSuccess[0]).isEqualTo(1);
        assertThat(failureOutcome[0]).isEqualTo(1);
        assertThat(executionExceptionOccurred).isTrue();
    }

    @Test
    public void testKeyAttestationApplicationPinNone() throws ExecutionException, InterruptedException {
        // Test  that when authentication type is set to APPLICATION_PIN and Key Attestation is NONE, device binding outcome is 'success'...
        // Make sure that the SDK DOES NOT include attestation data in the JWT...
        final int[] bindSuccess = {0};

        ActivityScenario<DummyActivity> scenario = ActivityScenario.launch(DummyActivity.class);
        scenario.onActivity(InitProvider::setCurrentActivity);

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "pin-none") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    callback.bind(context, deviceBindingAuthenticationType ->
                                    new ApplicationPinDeviceAuthenticator((prompt, fragmentActivity, $completion) -> "1234".toCharArray()),
                            new FRListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    bindSuccess[0]++;

                                    // Validate the jwt sent to AM...
                                    try {
                                        JWT jwt = JWTParser.parse((String) callback.getInputValue(0));
                                        String jwkAlg = ((SignedJWT) jwt).getHeader().getJWK().getAlgorithm().toString();
                                        String jwkUse = ((SignedJWT) jwt).getHeader().getJWK().getKeyUse().toString();
                                        List<Base64> x5c = ((SignedJWT) jwt).getHeader().getJWK().getX509CertChain();

                                        assertThat(jwkAlg).isEqualTo("RS512");
                                        assertThat(jwkUse).isEqualTo("sig");
                                        assertThat(x5c).isNull(); // When Android Key Attestation is set to NONE in AM

                                        /// Assert some other properties
                                        assertThat(jwt.getJWTClaimsSet().getClaim("iss")).isEqualTo("org.forgerock.android.auth.test");
                                        assertThat(jwt.getJWTClaimsSet().getClaim("platform")).isEqualTo("android");
                                        assertThat(jwt.getJWTClaimsSet().getClaim("android-version")).isEqualTo(Long.valueOf(Build.VERSION.SDK_INT));

                                    } catch (ParseException e) {
                                        throw new RuntimeException(e);
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

    @Test
    public void testKeyAttestationApplicationPinDefault() {
        // Test  that when authentication type is set to APPLICATION_PIN and Key Attestation is DEFAULT, device binding outcome is 'unsupported'...
        final int[] bindSuccess = {0};
        final int[] bindFail = {0};
        final int[] unsupportedOutcome = {0};
        boolean executionExceptionOccurred = false;

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "pin-default") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    callback.bind(context, deviceBindingAuthenticationType ->
                                    new ApplicationPinDeviceAuthenticator((prompt, fragmentActivity, $completion) -> "1234".toCharArray()),
                            new FRListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    bindSuccess[0]++;
                                    node.next(context, nodeListener);
                                }

                                @Override
                                public void onException(Exception e) {
                                    bindFail[0]++;
                                    assertThat(e.getMessage()).isEqualTo("Device not supported. Please verify the biometric or Pin settings");
                                    node.next(context, nodeListener);
                                }
                            });
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Unsupported");
                    unsupportedOutcome[0]++;
                    node.next(context, nodeListener);
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        // Ensure that the journey finishes with failure
        try {
            Assert.assertNull(nodeListenerFuture.get());
        } catch (ExecutionException e) {
            executionExceptionOccurred = true;
            assertThat(e.getMessage()).isEqualTo("ApiException{statusCode=401, error='', description='{\"code\":401,\"reason\":\"Unauthorized\",\"message\":\"Login failure\"}'}");
        } catch (InterruptedException e) {
            Assert.fail("Unexpected exception.");
        }
        Assert.assertNull(FRSession.getCurrentSession());

        assertThat(bindSuccess[0]).isEqualTo(0);
        assertThat(bindFail[0]).isEqualTo(1);
        assertThat(unsupportedOutcome[0]).isEqualTo(1);
        assertThat(executionExceptionOccurred).isTrue();
    }

    @Test
    public void testKeyAttestationApplicationPinCustom() {
        // Test  that when authentication type is set to APPLICATION_PIN and Key Attestation is CUSTOM, device binding outcome is 'unsupported'...
        final int[] bindSuccess = {0};
        final int[] bindFail = {0};
        final int[] unsupportedOutcome = {0};
        boolean executionExceptionOccurred = false;

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "pin-custom") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    callback.bind(context, deviceBindingAuthenticationType ->
                                    new ApplicationPinDeviceAuthenticator((prompt, fragmentActivity, $completion) -> "1234".toCharArray()),
                            new FRListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    bindSuccess[0]++;
                                    node.next(context, nodeListener);
                                }

                                @Override
                                public void onException(Exception e) {
                                    bindFail[0]++;
                                    assertThat(e.getMessage()).isEqualTo("Device not supported. Please verify the biometric or Pin settings");
                                    node.next(context, nodeListener);
                                }
                            });
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Unsupported");
                    unsupportedOutcome[0]++;
                    node.next(context, nodeListener);
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        // Ensure that the journey finishes with failure
        try {
            Assert.assertNull(nodeListenerFuture.get());
        } catch (ExecutionException e) {
            executionExceptionOccurred = true;
            assertThat(e.getMessage()).isEqualTo("ApiException{statusCode=401, error='', description='{\"code\":401,\"reason\":\"Unauthorized\",\"message\":\"Login failure\"}'}");
        } catch (InterruptedException e) {
            Assert.fail("Unexpected exception.");
        }
        Assert.assertNull(FRSession.getCurrentSession());

        assertThat(bindSuccess[0]).isEqualTo(0);
        assertThat(bindFail[0]).isEqualTo(1);
        assertThat(unsupportedOutcome[0]).isEqualTo(1);
        assertThat(executionExceptionOccurred).isTrue();
    }
}



