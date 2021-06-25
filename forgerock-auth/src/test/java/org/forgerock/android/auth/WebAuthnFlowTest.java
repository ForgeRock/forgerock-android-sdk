/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.fragment.app.FragmentActivity;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback;
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

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
    public void testAttachmentNotPlatform() throws InterruptedException, JSONException {
        enqueue("/webAuthn_registration_71_not_platform.json", HttpURLConnection.HTTP_OK);
        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        InitProvider.setCurrentActivity(fragmentActivity);

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {
            @Override
            public void onCallbackReceived(Node state) {
                try {
                    assertThat(state.getCallback(WebAuthnRegistrationCallback.class)).isNotNull();
                    state.getCallback(WebAuthnRegistrationCallback.class).register(state, this);
                } catch (Error e) {
                    throw new RuntimeException(e);
                }
                state.next(context, this);
            }
        };
        frAuth.next(context, nodeListenerFuture);
        try {
            nodeListenerFuture.get();
            failBecauseExceptionWasNotThrown(ExecutionException.class);
        } catch (ExecutionException e) {
            //Make sure the caller receive the correct exception
            assertThat(e.getCause()).isInstanceOf(UnsupportedOperationException.class);
        }

        //Make sure the server receive unsupported in HiddenValueCallback
        server.takeRequest(); //trigger the tree
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();
        JSONObject result = new JSONObject(body);
        assertThat(result.getJSONArray("callbacks").length()).isEqualTo(2);
        JSONObject hiddenValueCallback = result.getJSONArray("callbacks").getJSONObject(1);
        assertThat(hiddenValueCallback.getString("type")).isEqualTo("HiddenValueCallback");
        assertThat(hiddenValueCallback.getJSONArray("input").length()).isEqualTo(1);
        String value = hiddenValueCallback.getJSONArray("input").getJSONObject(0).getString("value");
        assertThat(value).isEqualTo("unsupported");
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
