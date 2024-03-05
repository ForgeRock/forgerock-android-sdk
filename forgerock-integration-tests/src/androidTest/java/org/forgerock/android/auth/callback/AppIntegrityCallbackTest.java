/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.assertj.core.api.Assertions.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class AppIntegrityCallbackTest  {

    protected static Context context = ApplicationProvider.getApplicationContext();

    protected final static String AM_URL = "https://openam-integrity1.forgeblocks.com/am";
    protected final static String REALM = "alpha";
    protected final static String OAUTH_CLIENT = "AndroidTest";
    protected final static String OAUTH_REDIRECT_URI = "org.forgerock.demo:/oauth2redirect";
    protected final static String SCOPE = "openid profile email address phone";

    protected final static String USERNAME = "sdkuser";
    protected final static String TREE = "TEST-app-integrity";

    @Rule
    public Timeout timeout = new Timeout(20000, TimeUnit.MILLISECONDS);

    @BeforeClass
    public static void setUpSDK() {
        Logger.set(Logger.Level.DEBUG);

        // Prepare dynamic configuration object
        FROptions options = FROptionsBuilder.build(builder -> {
            builder.server(serverBuilder -> {
                serverBuilder.setUrl(AM_URL);
                serverBuilder.setRealm(REALM);
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
    public void testAppIntegrityCallback() throws ExecutionException, InterruptedException {
        // This test checks the returned callback from the App Integrity node
        // Is also tests that the AppIntegrity node triggers correct "custom" client error outcome... (in this case "abort")
        final int[] abort = {0};

        NodeListenerFuture<FRSession> nodeListenerFuture = new AppIntegrityNodeListener(context, "default") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(AppIntegrityCallback.class) != null) {
                    AppIntegrityCallback callback = node.getCallback(AppIntegrityCallback.class);

                    assertThat(callback.getRequestType()).isEqualTo(RequestType.CLASSIC);
                    assertThat(callback.getProjectNumber()).isEqualTo("684644441808");
                    Assert.assertNotNull(callback.getNonce());

                    // Set "Abort" outcome...
                    callback.setClientError("Abort");
                    node.next(context, nodeListener);
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Abort");
                    abort[0]++;
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

        // Make sure AppIntegrity node fired the "abort" outcome
        assertThat(abort[0]).isEqualTo(1);
    }

    @Test
    public void testAppIntegrityClassic() throws ExecutionException, InterruptedException {
        // This test performs a CLASSIC api call
        final int[] successClientCall = {0};
        final int[] failureOutcome = {0};

        NodeListenerFuture<FRSession> nodeListenerFuture = new AppIntegrityNodeListener(context, "classic") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(AppIntegrityCallback.class) != null) {
                    AppIntegrityCallback callback = node.getCallback(AppIntegrityCallback.class);

                    // Perform app integrity check
                    callback.requestIntegrityToken(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            successClientCall[0]++;
                            node.next(context, nodeListener);

                        }
                        @Override
                        public void onException(Exception e) {
                            Assertions.fail("Unexpected failure during client app integrity call!");
                            node.next(context, nodeListener);
                        }
                    });

                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    // The node configuration in this case sets the verdict variable in the shared stated
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Failure");
                    failureOutcome[0]++;
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

        assertThat(successClientCall[0]).isEqualTo(1);
        assertThat(failureOutcome[0]).isEqualTo(1);
    }

    @Test
    public void testAppIntegrityStandard() throws ExecutionException, InterruptedException {
        // This test performs a STANDARD api call
        final int[] successClientCall = {0};
        final int[] failureOutcome = {0};

        NodeListenerFuture<FRSession> nodeListenerFuture = new AppIntegrityNodeListener(context, "standard") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(AppIntegrityCallback.class) != null) {
                    AppIntegrityCallback callback = node.getCallback(AppIntegrityCallback.class);

                    // Perform app integrity check
                    callback.requestIntegrityToken(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            successClientCall[0]++;
                            node.next(context, nodeListener);

                        }
                        @Override
                        public void onException(Exception e) {
                            Assertions.fail("Unexpected failure during client app integrity call!");
                            node.next(context, nodeListener);
                        }
                    });

                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    // The node configuration in this case sets the verdict variable in the shared stated
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Failure");
                    failureOutcome[0]++;
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

        assertThat(successClientCall[0]).isEqualTo(1);
        assertThat(failureOutcome[0]).isEqualTo(1);
    }

    @Test
    public void testAppIntegrityVerdictVarON() throws ExecutionException, InterruptedException {
        // This test checks if VERDICT variable is set in the shared stated when enabled...
        final int[] verdictExists = {0};

        NodeListenerFuture<FRSession> nodeListenerFuture = new AppIntegrityNodeListener(context, "verdict-on") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(AppIntegrityCallback.class) != null) {
                    AppIntegrityCallback callback = node.getCallback(AppIntegrityCallback.class);

                    // Perform app integrity check
                    callback.requestIntegrityToken(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            node.next(context, nodeListener);
                        }
                        @Override
                        public void onException(Exception e) {
                            Assertions.fail("Unexpected failure during client app integrity call!");
                        }
                    });
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    // The node configuration in this case sets the verdict variable in the shared stated
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Verdict Exists");
                    verdictExists[0]++;
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

        assertThat(verdictExists[0]).isEqualTo(1);
    }

    @Test
    public void testAppIntegrityVerdictVarOFF() throws ExecutionException, InterruptedException {
        // This test checks if VERDICT variable is set in the shared stated - in this case the node configuration is set to OFF
        final int[] verdictDoesNOTexist = {0};

        NodeListenerFuture<FRSession> nodeListenerFuture = new AppIntegrityNodeListener(context, "verdict-off") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(AppIntegrityCallback.class) != null) {
                    AppIntegrityCallback callback = node.getCallback(AppIntegrityCallback.class);

                    callback.requestIntegrityToken(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            node.next(context, nodeListener);
                        }
                        @Override
                        public void onException(Exception e) {
                            Assertions.fail("Unexpected failure during client app integrity call!");
                            node.next(context, nodeListener);
                        }
                    });
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    // The node configuration in this case sets the verdict variable in the shared stated
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Verdict DOES NOT exist");
                    verdictDoesNOTexist[0]++;
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

        assertThat(verdictDoesNOTexist[0]).isEqualTo(1);
    }

    @Test
    public void testAppIntegrityClientError() throws ExecutionException, InterruptedException {
        // Verifies that client errors a properly handled by the SDK and trigger the default outcome of the Integrity node
        final int[] clientError = {0};

        NodeListenerFuture<FRSession> nodeListenerFuture = new AppIntegrityNodeListener(context, "client-error") {
            final NodeListener<FRSession> nodeListener = this;

            @Override
            public void onCallbackReceived(Node node) {
                if (node.getCallback(AppIntegrityCallback.class) != null) {
                    AppIntegrityCallback callback = node.getCallback(AppIntegrityCallback.class);

                    // Perform app integrity check
                    callback.requestIntegrityToken(context, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Assertions.fail("Unexpected successful integrity call!");
                            node.next(context, nodeListener);
                        }
                        @Override
                        public void onException(Exception e) {
                            // Don't do much...
                            Logger.debug("testAppIntegrityClientError", e.getMessage());
                            node.next(context, nodeListener);
                        }
                    });
                    return;
                }
                if (node.getCallback(TextOutputCallback.class) != null) {
                    // The node configuration in this case sets the verdict variable in the shared stated
                    TextOutputCallback callback = node.getCallback(TextOutputCallback.class);
                    assertThat(callback.getMessage()).isEqualTo("Client Error");
                    clientError[0]++;
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

        assertThat(clientError[0]).isEqualTo(1);
    }
}



