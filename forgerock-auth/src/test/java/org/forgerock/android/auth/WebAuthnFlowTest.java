/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback;
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class WebAuthnFlowTest extends BaseTest {

    private FRAuth frAuth;

    @Before
    public void setUpFRAuth() {
        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .serverConfig(serverConfig)
                .context(context)
                .encryptor(new MockEncryptor())
                .build();

        frAuth = FRAuth.builder()
                .serviceName("Example")
                .context(context)
                .sessionManager(SessionManager.builder()
                        .tokenManager(Config.getInstance().getTokenManager())
                        .singleSignOnManager(singleSignOnManager)
                        .build())
                .serverConfig(serverConfig)
                .build();

    }

    @Test
    public void testDerivedRegistrationCallback() throws ExecutionException, InterruptedException {
        enqueue("/webAuthn_registration_71.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {
            @Override
            public void onCallbackReceived(Node state) {
                try {
                    assertThat(state.getCallback(WebAuthnRegistrationCallback.class)).isNotNull();
                    assertThat(state.getCallback(WebAuthnRegistrationCallback.class)
                            .findHiddenValueCallback(state)).isNotNull();
                } catch (Error e) {
                    throw new RuntimeException(e);
                }
                state.next(context, this);
            }
        };
        frAuth.next(context, nodeListenerFuture);
        nodeListenerFuture.get();
    }

    @Test
    public void testDerivedAuthenticationCallback() throws ExecutionException, InterruptedException {
        enqueue("/webAuthn_authentication_71.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {
            @Override
            public void onCallbackReceived(Node state) {
                try {
                    assertThat(state.getCallback(WebAuthnAuthenticationCallback.class)).isNotNull();
                    assertThat(state.getCallback(WebAuthnAuthenticationCallback.class)
                            .findHiddenValueCallback(state)).isNotNull();
                } catch (Error e) {
                    throw new RuntimeException(e);
                }
                state.next(context, this);
            }
        };
        frAuth.next(context, nodeListenerFuture);
        nodeListenerFuture.get();
    }
}
