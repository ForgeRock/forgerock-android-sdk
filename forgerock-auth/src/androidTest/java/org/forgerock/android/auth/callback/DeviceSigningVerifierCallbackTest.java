/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.FRAuth;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FROptions;
import org.forgerock.android.auth.FROptionsBuilder;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;
import org.forgerock.android.auth.NodeListenerFuture;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.text.ParseException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class DeviceSigningVerifierCallbackTest {
    private static final String TAG = DeviceSigningVerifierCallbackTest.class.getSimpleName();
    protected static Context context = ApplicationProvider.getApplicationContext();

    // This test uses dynamic configuration with the following settings:
    protected final static String AM_URL = "https://openam-dbind.forgeblocks.com/am";
    protected final static String REALM = "alpha";
    protected final static String OAUTH_CLIENT = "AndroidTest";
    protected final static String OAUTH_REDIRECT_URI = "org.forgerock.demo:/oauth2redirect";
    protected final static String SCOPE = "openid profile email address phone";
    protected final static String TREE = "device-verifier";

    protected final static String USERNAME = "sdkuser";
    protected static String KID = null; // Used to store the kid of the key generated during binding
    protected static String USER_ID = null; // Used to store the userId of the user who binds the device

    @Rule
    public Timeout timeout = new Timeout(10000, TimeUnit.MILLISECONDS);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUpSDK() throws ExecutionException, InterruptedException {
        Logger.set(Logger.Level.DEBUG);

        // Prepare dynamic configuration object
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

        final int[] bindSuccess = {0};
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "bind") {
            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(DeviceBindingCallback.class) != null) {
                    DeviceBindingCallback callback = node.getCallback(DeviceBindingCallback.class);
                    NodeListener<FRSession> nodeListener = this;
                    USER_ID = callback.getUserId();
                    // Bind the device...
                    callback.bind(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            bindSuccess[0]++;
                        }

                        @Override
                        public void onException(Exception e) {
                            Assertions.fail(e.getMessage());
                        }
                    });
                    node.next(context, nodeListener);

                    // Get the kid
                    try {
                        KID = JWTParser.parse((String) callback.getInputValue(0)).getHeader().toJSONObject().get("kid").toString();
                        Logger.debug("Test", KID);
                    } catch (ParseException e) {
                        Logger.debug(TAG, e.getMessage());
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

    @After
    public void logoutSession() {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
        CallbackFactory.getInstance().register(DeviceSigningVerifierCallback.class);
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
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);

                    NodeListener<FRSession> nodeListener = this;
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

                    NodeListener<FRSession> nodeListener = this;
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
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(DeviceSigningVerifierCallback.class) != null) {
                    DeviceSigningVerifierCallback callback = node.getCallback(DeviceSigningVerifierCallback.class);

                    NodeListener<FRSession> nodeListener = this;
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

                    NodeListener<FRSession> nodeListener = this;
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

                    NodeListener<FRSession> nodeListener = this;
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Success");
                    authSuccess[0]++;

                    NodeListener<FRSession> nodeListener = this;
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

                    NodeListener<FRSession> nodeListener = this;
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Success");
                    authSuccess[0]++;

                    NodeListener<FRSession> nodeListener = this;
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
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(CustomDeviceSigningVerifierCallback.class) != null) {
                    CustomDeviceSigningVerifierCallback callback = node.getCallback(CustomDeviceSigningVerifierCallback.class);
                    callback.setExpSeconds(90);

                    NodeListener<FRSession> nodeListener = this;
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
                            } catch (ParseException e) {
                                Assertions.fail("Invalid JWT: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onException(Exception e) {
                            Assertions.fail(e.getMessage());
                        }
                    });
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Success");
                    authSuccess[0]++;

                    NodeListener<FRSession> nodeListener = this;
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
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(CustomDeviceSigningVerifierCallback.class) != null) {
                    CustomDeviceSigningVerifierCallback callback = node.getCallback(CustomDeviceSigningVerifierCallback.class);
                    // Make the exp value to "now"
                    callback.setExpSeconds(0);

                    callback.sign(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            signSuccess[0]++;
                        }

                        @Override
                        public void onException(Exception e) {
                            Assertions.fail(e.getMessage());
                        }
                    });

                    NodeListener<FRSession> nodeListener = this;
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Failure");
                    failureOutcome[0]++;

                    NodeListener<FRSession> nodeListener = this;
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
        final int[] signSuccess = {0};
        final int[] failureOutcome = {0};

        CallbackFactory.getInstance().register(CustomDeviceSigningVerifierCallback.class);
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
            @Override
            public void onCallbackReceived(Node node)
            {
                if (node.getCallback(CustomDeviceSigningVerifierCallback.class) != null) {
                    CustomDeviceSigningVerifierCallback callback = node.getCallback(CustomDeviceSigningVerifierCallback.class);
                    String customJwt = callback.getSignedJwt(
                            "ala-bala",
                            callback.getUserId(),
                            "invalid-challenge");

                    callback.setJws(customJwt);
                    NodeListener<FRSession> nodeListener = this;
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Failure");
                    failureOutcome[0]++;

                    NodeListener<FRSession> nodeListener = this;
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
    public void testDeviceVerificationFailureKeyNotFound() throws ExecutionException, InterruptedException {
        final int[] keyNotFoundOutcome = {0};

        CallbackFactory.getInstance().register(CustomDeviceSigningVerifierCallback.class);
        NodeListenerFuture<FRSession> nodeListenerFuture = new DeviceSigningVerifierNodeListener(context, "default")
        {
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

                    NodeListener<FRSession> nodeListener = this;
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Key Not Found");
                    keyNotFoundOutcome[0]++;

                    NodeListener<FRSession> nodeListener = this;
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

                    NodeListener<FRSession> nodeListener = this;
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Failure");
                    authSuccess[0]++;

                    NodeListener<FRSession> nodeListener = this;
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
                        }
                        @Override
                        public void onException(Exception e) {
                            Assertions.fail(e.getMessage());
                        }
                    });

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
                        String temperedPayload = Base64.getEncoder().encodeToString(temperedPayloadJson.toString().getBytes());
                        String temperedJwt = new StringBuilder().
                                append(jwtHeader).append(".").
                                append(temperedPayload).append(".").
                                append(jwtSignature).toString();

                        Logger.debug(DeviceSigningVerifierCallbackTest.class.getSimpleName(), "Original JWT: " + callback.getInputValue(0));
                        Logger.debug(DeviceSigningVerifierCallbackTest.class.getSimpleName(), "Tempered JWT: " + temperedJwt);

                        // Overwrite the JWT input value in the callback to AM...
                        callback.setJws(temperedJwt);
                    } catch (ParseException | JSONException e) {
                        Logger.debug(TAG, e.getMessage());
                    }

                    NodeListener<FRSession> nodeListener = this;
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback textOutputCallback = node.getCallback(TextOutputCallback.class);
                    assertThat(textOutputCallback.getMessage()).isEqualTo("Failure");
                    failureOutcome[0]++;

                    NodeListener<FRSession> nodeListener = this;
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

class DeviceSigningVerifierNodeListener extends NodeListenerFuture<FRSession> {

    private Context context;
    private String nodeConfiguration;

    public DeviceSigningVerifierNodeListener(Context context, String nodeConfiguration) {
        this.context = context;
        this.nodeConfiguration = nodeConfiguration;
    }

    @Override
    public void onCallbackReceived(Node node) {
        if (node.getCallback(ChoiceCallback.class) != null) {
            ChoiceCallback choiceCallback = node.getCallback(ChoiceCallback.class);
            List<String> choices = choiceCallback.getChoices();
            // Explicitly set the first choice collector to "collectusername" for non "usernameless" flows
            if (choices.contains("usernameless") && !nodeConfiguration.equals("usernameless")) {
                int choiceIndex = choices.indexOf("collectusername");
                choiceCallback.setSelectedIndex(choiceIndex);
                node.next(context, this);
                return;
            }
            int choiceIndex = choices.indexOf(nodeConfiguration);
            choiceCallback.setSelectedIndex(choiceIndex);
            node.next(context, this);
        }
        if (node.getCallback(NameCallback.class) != null) {
            node.getCallback(NameCallback.class).setName(DeviceSigningVerifierCallbackTest.USERNAME);
            node.next(context, this);
        }
    }
}
