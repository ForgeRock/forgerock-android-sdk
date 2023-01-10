/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.assertThat;

import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.NodeListenerFuture;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class DeviceSigningVerifierCallbackTest extends BaseDeviceBindingTest {
    protected final static String TREE = "device-verifier";

    @BeforeClass
    public static void bindDevice() throws ExecutionException, InterruptedException {
        final int[] bindSuccess = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "bind")
        {
            final NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);

                    USER_ID = callback.getUserId();
                    // Bind the device...
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

        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());

        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
    }

    @Test
    public void testDeviceSigningVerifierUnknownUserError() throws ExecutionException, InterruptedException {
        final int[] hit = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);

                    Assertions.fail("Test failed: Received unexpected DeviceSigningVerifierCallback! (see SDKS-2169)" );
                    return;
                }
                if (node.getCallback(NameCallback.class) != null) {
                    hit[0]++;
                    node.getCallback(NameCallback.class).setName("UNKNOWN-USER");
                    node.next(context, this);
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

        assertThat(hit[0]).isEqualTo(1);
    }

    @Test
    public void testDeviceSigningVerifierCallbackDefaults() throws ExecutionException, InterruptedException {
        final int[] hit = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
            final NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);
                    Assert.assertNotNull(callback.getUserId());
                    Assert.assertNotNull(callback.getChallenge());
                    assertThat(callback.getTitle()).isEqualTo("Authentication required");
                    assertThat(callback.getSubtitle()).isEqualTo("Cryptography device binding");
                    assertThat(callback.getDescription()).isEqualTo("Please complete with biometric to proceed");
                    assertThat(callback.getTimeout()).isEqualTo(60);

                    // Set "Abort" outcome (without signing the challenge), so that the journey finishes...
                    callback.setClientError("Abort");
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Abort");
                    hit[0]++;

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
        assertThat(hit[0]).isEqualTo(1);
    }

    @Test
    public void testDeviceSigningVerifierCallbackCustom() throws ExecutionException, InterruptedException {
        final int[] hit = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "custom")
        {
            final NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);

                    Assert.assertNotNull(callback.getUserId());
                    Assert.assertNotNull(callback.getChallenge());

                    assertThat(callback.getChallenge()).isEqualTo("my-hardcoded-challenge");
                    assertThat(callback.getTitle()).isEqualTo("Custom Title");
                    assertThat(callback.getSubtitle()).isEqualTo("Custom Subtitle");
                    assertThat(callback.getDescription()).isEqualTo("Custom Description");
                    assertThat(callback.getTimeout()).isEqualTo(10);

                    // Set "Custom" client error (without signing the challenge), so that the journey finishes...
                    callback.setClientError("Custom");
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Custom");
                    hit[0]++;

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
        assertThat(hit[0]).isEqualTo(1);
    }

    @Test
    public void testDeviceVerificationSuccess() throws ExecutionException, InterruptedException {
        final int[] signSuccess = {0};
        final int[] authSuccess = {0};

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);

                    Assert.assertNotNull(callback.getUserId());
                    Assert.assertNotNull(callback.getChallenge());

                    callback.sign(context, new FRListener<Void>() {
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
    public void testDeviceVerificationUsernamelessSuccess() throws ExecutionException, InterruptedException {
        final int[] signSuccess = {0};
        final int[] authSuccess = {0};

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "usernameless")
        {
            final NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);

                    // In usernameless userId in the callback is empty...
                    assertThat(callback.getUserId()).isEmpty();
                    Assert.assertNotNull(callback.getChallenge());

                    callback.sign(context, new FRListener<Void>() {
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

    @Test
    public void testDeviceVerificationSuccessCustomExp() throws ExecutionException, InterruptedException {
        final int[] signSuccess = {0};
        final int[] authSuccess = {0};

        CallbackFactory.getInstance().register(CustomDeviceSigningVerifierCallback.class);

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
            final NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(CustomDeviceSigningVerifierCallback.class) != null) {
                    CustomDeviceSigningVerifierCallback callback = node.getCallback(CustomDeviceSigningVerifierCallback.class);
                    callback.setExpSeconds(90);

                    Assert.assertNotNull(callback.getUserId());
                    Assert.assertNotNull(callback.getChallenge());

                    callback.sign(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            // Verify the JWT
                            try {
                                Calendar expMin = Calendar.getInstance();
                                Calendar expMax = Calendar.getInstance();
                                expMin.add(Calendar.SECOND, 85);
                                expMax.add(Calendar.SECOND, 90);

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
    public void testDeviceVerificationFailureExpiredJwt() throws ExecutionException, InterruptedException {
        final int[] signSuccess = {0};
        final int[] failureOutcome = {0};

        CallbackFactory.getInstance().register(CustomDeviceSigningVerifierCallback.class);

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
            final NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(CustomDeviceSigningVerifierCallback.class) != null) {
                    CustomDeviceSigningVerifierCallback callback = node.getCallback(CustomDeviceSigningVerifierCallback.class);
                    // Make the exp value to 2 minute in the past
                    callback.setExpSeconds(-120);

                    callback.sign(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            signSuccess[0]++;
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
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(signSuccess[0]).isEqualTo(1);
        assertThat(failureOutcome[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testDeviceVerificationFailureInvalidChallenge() throws ExecutionException, InterruptedException {
        final int[] failureOutcome = {0};

        CallbackFactory.getInstance().register(CustomDeviceSigningVerifierCallback.class);
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
            final NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(CustomDeviceSigningVerifierCallback.class) != null) {
                    CustomDeviceSigningVerifierCallback callback = node.getCallback(CustomDeviceSigningVerifierCallback.class);
                    String customJwt = callback.getSignedJwt(
                            KID,
                            callback.getUserId(),
                            "invalid-challenge");

                    callback.setJws(customJwt);
                    node.next(context, nodeListener);
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
        Assert.assertNotNull(nodeListenerFuture.get());

        assertThat(failureOutcome[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testDeviceVerificationFailureKeyNotFound() throws ExecutionException, InterruptedException {
        final int[] keyNotFoundOutcome = {0};

        CallbackFactory.getInstance().register(CustomDeviceSigningVerifierCallback.class);
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
            final NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(CustomDeviceSigningVerifierCallback.class) != null) {
                    CustomDeviceSigningVerifierCallback callback = node.getCallback(CustomDeviceSigningVerifierCallback.class);
                    callback.setExpSeconds(90);
                    // Prepare a signed JWT with a key unknown to AM
                    String customJwt = callback.getSignedJwt(
                                                "ala-bala",
                                                    callback.getUserId(),
                                                    callback.getChallenge());

                    callback.setJws(customJwt);
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Key Not Found");
                    keyNotFoundOutcome[0]++;
                    node.next(context, nodeListener);
                    return;
                }

                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(keyNotFoundOutcome[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testDeviceVerificationFailureInvalidSignKey() throws ExecutionException, InterruptedException {
        final int[] authSuccess = {0};

        CallbackFactory.getInstance().register(CustomDeviceSigningVerifierCallback.class);
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
            final NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(CustomDeviceSigningVerifierCallback.class) != null) {
                    CustomDeviceSigningVerifierCallback callback = node.getCallback(CustomDeviceSigningVerifierCallback.class);
                    callback.setExpSeconds(90);
                    String customJwt = callback.getSignedJwt(
                            KID,
                            callback.getUserId(),
                            callback.getChallenge());

                    callback.setJws(customJwt);
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Failure");
                    authSuccess[0]++;

                    node.next(context, nodeListener);
                    return;
                }

                super.onCallbackReceived(node);
            }
        };

        FRSession.authenticate(context, TREE, nodeListenerFuture);
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(authSuccess[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void testDeviceVerificationFailureTemperedJwt() throws ExecutionException, InterruptedException {
        final int[] signSuccess = {0};
        final int[] failureOutcome = {0};

        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
            final NodeListener<FRSession> nodeListener = this;
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);

                    Assert.assertNotNull(callback.getUserId());
                    Assert.assertNotNull(callback.getChallenge());

                    callback.sign(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            signSuccess[0]++;
                            // Prepare prepare exp value for 10 days ahead of now...
                            Calendar expTempered = Calendar.getInstance();
                            expTempered.add(Calendar.SECOND, 864000);

                            try {
                                String jwtHeader = JWTParser.parse((String) callback.getInputValue(0)).getParsedParts()[0].toString();
                                String jwtPayload = JWTParser.parse((String) callback.getInputValue(0)).getJWTClaimsSet().toPayload().toString();
                                String jwtSignature = JWTParser.parse((String) callback.getInputValue(0)).getParsedParts()[2].toString();

                                // Temper the exp value in the original JWT...
                                // The Device Signing Verifier node should detect that the payload has been tempered and therefore should fail!
                                JSONObject temperedPayloadJson = new JSONObject(jwtPayload);
                                temperedPayloadJson.put("exp", expTempered.getTime().getTime());
                                String temperedPayload = Base64.encodeToString(temperedPayloadJson.toString().getBytes(), Base64.DEFAULT);
                                String temperedJwt = new StringBuilder().
                                        append(jwtHeader).append(".").
                                        append(temperedPayload).append(".").
                                        append(jwtSignature).toString();

                                Logger.debug(TAG, "Original JWT: " + callback.getInputValue(0));
                                Logger.debug(TAG, "Tempered JWT: " + temperedJwt);

                                // Overwrite the JWT input value in the callback to AM...
                                callback.setJws(temperedJwt);
                            } catch (ParseException | JSONException e) {
                                Logger.debug(TAG, e.getMessage());
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
        Assert.assertNotNull(nodeListenerFuture.get());
        assertThat(signSuccess[0]).isEqualTo(1);
        assertThat(failureOutcome[0]).isEqualTo(1);

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }
}
