/*
 * Copyright (c) 2022 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.assertThat;

import android.os.Build;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

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
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)  // Key Attestation is only supported on Android 7.0 (API level 24) and above...
public class KeyAttestationTest extends BaseDeviceBindingTest {
    protected final static String TREE = "key-attestation";

    @Test
    public void testKeyAttestationNoneAttestationOff() throws ExecutionException, InterruptedException {
        // Test that when "Key Attestation" is OFF in AM, the SDK does not include x5c (X.509 Certificate Chain) parameter in the JWK...
        final int[] bindSuccess = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "none-attestation-off") {
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
                                assertThat(jwt.getJWTClaimsSet().getClaim("iss")).isEqualTo("org.forgerock.android.integration.test");
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
    public void testKeyAttestationNoneAttestationOn() throws ExecutionException, InterruptedException {
        // Make sure that when "Key Attestation" is ON in AM, the SDK includes x5c (X.509 Certificate Chain) parameter...
        final int[] bindSuccess = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "none-attestation-on") {
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
                                assertThat(jwt.getJWTClaimsSet().getClaim("iss")).isEqualTo("org.forgerock.android.integration.test");
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
    public void testKeyAttestationTransientStateVariable() throws ExecutionException, InterruptedException {
        // Ensure that when Key Attestation toggle button is enabled in the Device Binding node,
        // Key Attestation Validation will be performed, and the extension data will be put into the transient state with the variable
        // DeviceBindingCallback.ATTESTATION
        final int[] bindSuccess = {0};
        final int[] attestationVarExists = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "attestation-var-set") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    callback.bind(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bindSuccess[0]++;

                            // Validate the jwt sent to AM includes attestation data
                            try {
                                JWT jwt = JWTParser.parse((String) callback.getInputValue(0));
                                String jwkAlg = ((SignedJWT) jwt).getHeader().getJWK().getAlgorithm().toString();
                                String jwkUse = ((SignedJWT) jwt).getHeader().getJWK().getKeyUse().toString();
                                List<Base64> x5c = ((SignedJWT) jwt).getHeader().getJWK().getX509CertChain();

                                assertThat(jwkAlg).isEqualTo("RS512");
                                assertThat(jwkUse).isEqualTo("sig");
                                assertThat(x5c).isNotNull(); // When Android Key Attestation is set to CUSTOM in AM

                                /// Assert some other properties
                                assertThat(jwt.getJWTClaimsSet().getClaim("iss")).isEqualTo("org.forgerock.android.integration.test");
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
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Attestation var exists");
                    attestationVarExists[0]++;
                    node.next(context, nodeListener);
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(bindSuccess[0]).isEqualTo(1); /// Make sure that device binding was successful...
        assertThat(attestationVarExists[0]).isEqualTo(1); /// Make sure that attestation variable exists in transient state

        // Ensure that the journey finishes with success - this also means that the attestation transient state variable is set in AM!
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testKeyAttestationTransientStateVariableNull() throws ExecutionException, InterruptedException {
        // Ensure that when Key Attestation toggle button is NOT enabled in the Device Binding node,
        // Key Attestation Validation will NOT be performed- transient variable DeviceBindingCallback.ATTESTATION should be null!
        final int[] bindSuccess = {0};
        final int[] attestationVarDoesNotExist = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "attestation-var-null") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    callback.bind(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bindSuccess[0]++;

                            // Validate the jwt sent to AM does NOT include attestation data
                            try {
                                JWT jwt = JWTParser.parse((String) callback.getInputValue(0));
                                String jwkAlg = ((SignedJWT) jwt).getHeader().getJWK().getAlgorithm().toString();
                                String jwkUse = ((SignedJWT) jwt).getHeader().getJWK().getKeyUse().toString();
                                List<Base64> x5c = ((SignedJWT) jwt).getHeader().getJWK().getX509CertChain();

                                assertThat(jwkAlg).isEqualTo("RS512");
                                assertThat(jwkUse).isEqualTo("sig");
                                assertThat(x5c).isNull(); // When Android Key Attestation is NULL
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
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Attestation var DOES NOT exist");
                    attestationVarDoesNotExist[0]++;
                    node.next(context, nodeListener);
                    return;
                }
                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(bindSuccess[0]).isEqualTo(1); /// Make sure that device binding was successful...
        assertThat(attestationVarDoesNotExist[0]).isEqualTo(1); /// Make sure that attestation variable does NOT exist in transient state

        // Ensure that the journey finishes with success - this also means that the attestation transient state variable is set in AM!
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testKeyAttestationApplicationPinAttestationOff() throws ExecutionException, InterruptedException {
        // Test  that when authentication type is set to APPLICATION_PIN and Key Attestation is OFF, device binding outcome is 'success'...
        // Make sure that the SDK DOES NOT include attestation data in the JWT...
        final int[] bindSuccess = {0};

        ActivityScenario<DummyActivity> scenario = ActivityScenario.launch(DummyActivity.class);
        scenario.onActivity(InitProvider::setCurrentActivity);

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "pin-attestation-off") {
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
                                        assertThat(jwt.getJWTClaimsSet().getClaim("iss")).isEqualTo("org.forgerock.android.integration.test");
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
    public void testKeyAttestationApplicationPinAttestationOn() {
        // Test  that when authentication type is set to APPLICATION_PIN and Key Attestation is ON, device binding outcome is 'unsupported'...
        final int[] bindSuccess = {0};
        final int[] bindFail = {0};
        final int[] unsupportedOutcome = {0};
        boolean executionExceptionOccurred = false;

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceBindingNodeListener(context, "pin-attestation-on") {
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



