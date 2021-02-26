/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn;

import android.content.Intent;
import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.fido.Fido;
import com.google.android.gms.fido.fido2.api.common.ErrorCode;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.FRSession;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListenerFuture;
import org.forgerock.android.auth.callback.CallbackFactory;
import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback;
import org.forgerock.android.auth.exception.WebAuthnResponseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class WebAuthnAuthenticationAndroidTest extends WebAuthnTest {

    private static final String SUCCESS_FIDO2_KEY_RESPONSE_EXTRA = "RU___6wBAAACAP__SAAAAEEAAAABqtuz6WWwU9CpCSoQ7VBfR_qGFLqBTjyTeOMnMbHQP_vVWblc6mX6txWpNqsCaHq64kUT_aUJTHJWNhh80KjvbgAAAAMA___MAAAAxwAAAHsidHlwZSI6IndlYmF1dGhuLmdldCIsImNoYWxsZW5nZSI6IklOWVRZWWdkcmozdzlaNXZYZDkwZ0VvSV9KaWNJenpkeVJVWUZSQkY3MjQiLCJvcmlnaW4iOiJhbmRyb2lkOmFway1rZXktaGFzaDpSOHhPN3JsUVdhV0w0QmxGeWdwdFdSYjVxY0tXZGZqelpJYVNSaXQ5WFZ3IiwiYW5kcm9pZFBhY2thZ2VOYW1lIjoib3JnLmZvcmdlcm9jay5hdXRoIn0ABAD__ywAAAAlAAAAfYgTsKOuctyA-tH5TlFITpPlsLXQlZPUTb-7RT3tniEFAAAAAQAAAAUA__9MAAAASAAAADBGAiEA_cn5srrQ7-JR7yrwPIGmJfx5rMkcZKPsAKo5BoEW1fICIQDwORk5kobppwcnbJ9Nm-ZGyEJ1eMupvPtwUr5BT5peLg";
    private static final String ERROR_FIDO2_KEY_ERROR = "RU___2gAAAACAAQAEgAAAAMA__9YAAAAKAAAAFQAaABlACAAaQBuAGMAbwBtAGkAbgBnACAAcgBlAHEAdQBlAHMAdAAgAGMAYQBuAG4AbwB0ACAAYgBlACAAdgBhAGwAaQBkAGEAdABlAGQAAAAAAA";

    @Test
    public void testDerivedAuthenticationCallback() throws ExecutionException, InterruptedException {
        enqueue("/webAuthn_authentication_71.json", HttpURLConnection.HTTP_OK);
        enqueue("/success.json", HttpURLConnection.HTTP_OK);

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {
            @Override
            public void onCallbackReceived(Node state) {
                try {
                    Assertions.assertThat(state.getCallback(WebAuthnAuthenticationCallback.class)).isNotNull();
                } catch (Error e) {
                    throw new RuntimeException(e);
                }
                state.next(context, this);
            }
        };
        FRSession.authenticate(context, "", nodeListenerFuture);
        nodeListenerFuture.get();
    }

    @Test
    public void testWebAuthnAuthenticationHappyPath() throws ExecutionException, InterruptedException, JSONException {

        enqueue("/webAuthn_authentication_71.json", HttpURLConnection.HTTP_OK);
        enqueue("/success.json", HttpURLConnection.HTTP_OK);
        CallbackFactory.getInstance().register(MockWebAuthnAuthenticationCallback.class);

        CountDownLatch latch = new CountDownLatch(1);
        final WebAuthnAuthenticationCallback[] callback = {null};
        final Node[] node = {null};

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture() {
            @Override
            public void onCallbackReceived(Node state) {
                    callback[0] = state.getCallback(MockWebAuthnAuthenticationCallback.class);
                    node[0] = state;
                    latch.countDown();
            }
        };

        FRSession.authenticate(context, "", nodeListenerFuture);
        latch.await();

        callback[0].authenticate(node[0], null, new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                node[0].next(context, nodeListenerFuture);
            }

            @Override
            public void onException(Exception e) {
                Assertions.fail(e.getMessage());
            }
        });

        sleepForFragmentToCommit();

        Intent intent = new Intent();
        intent.putExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA, Base64.decode(SUCCESS_FIDO2_KEY_RESPONSE_EXTRA, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING ));
        getFragment().onActivityResult(WebAuthnHeadlessAuthenticateFragment.REQUEST_FIDO2_SIGNIN,
                0, intent);


        FRSession session = nodeListenerFuture.get();
        Assertions.assertThat(session).isNotNull();

        RecordedRequest request = server.takeRequest(); //first Request
        request = server.takeRequest(); //webauthn authentication
        String body = request.getBody().readUtf8();
        JSONObject result = new JSONObject(body);
        assertThat(result.getJSONArray("callbacks").length()).isEqualTo(2);
        JSONObject hiddenValueCallback = result.getJSONArray("callbacks").getJSONObject(1);
        assertThat(hiddenValueCallback.getString("type")).isEqualTo("HiddenValueCallback");
        assertThat(hiddenValueCallback.getJSONArray("input").length()).isEqualTo(1);
        String value = hiddenValueCallback.getJSONArray("input").getJSONObject(0).getString("value");
        assertThat(value).isEqualTo("{\"type\":\"webauthn.get\",\"challenge\":\"INYTYYgdrj3w9Z5vXd90gEoI_JicIzzdyRUYFRBF724\",\"origin\":\"android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw\",\"androidPackageName\":\"org.forgerock.auth\"}::125,-120,19,-80,-93,-82,114,-36,-128,-6,-47,-7,78,81,72,78,-109,-27,-80,-75,-48,-107,-109,-44,77,-65,-69,69,61,-19,-98,33,5,0,0,0,1::48,70,2,33,0,-3,-55,-7,-78,-70,-48,-17,-30,81,-17,42,-16,60,-127,-90,37,-4,121,-84,-55,28,100,-93,-20,0,-86,57,6,-127,22,-43,-14,2,33,0,-16,57,25,57,-110,-122,-23,-89,7,39,108,-97,77,-101,-26,70,-56,66,117,120,-53,-87,-68,-5,112,82,-66,65,79,-102,94,46::Aarbs-llsFPQqQkqEO1QX0f6hhS6gU48k3jjJzGx0D_71Vm5XOpl-rcVqTarAmh6uuJFE_2lCUxyVjYYfNCo724");

    }

    @Test
    public void testWebAuthnAuthenticationError() throws InterruptedException, JSONException {

        enqueue("/webAuthn_authentication_71.json", HttpURLConnection.HTTP_OK);
        CallbackFactory.getInstance().register(MockWebAuthnAuthenticationCallback.class);

        CountDownLatch latch = new CountDownLatch(1);
        final WebAuthnAuthenticationCallback[] callback = {null};
        final Node[] node = {null};

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture() {
            @Override
            public void onCallbackReceived(Node state) {
                callback[0] = state.getCallback(MockWebAuthnAuthenticationCallback.class);
                node[0] = state;
                latch.countDown();
            }
        };

        FRSession.authenticate(context, "", nodeListenerFuture);
        latch.await();

        CountDownLatch authenticationLatch = new CountDownLatch(1);
        callback[0].authenticate(node[0], null,  new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                authenticationLatch.countDown();
                fail();
            }

            @Override
            public void onException(Exception e) {
                assertTrue(e instanceof WebAuthnResponseException);
                assertEquals(ErrorCode.SECURITY_ERR, ((WebAuthnResponseException) e).getErrorCode());
                assertEquals(ErrorCode.SECURITY_ERR.getCode(), ((WebAuthnResponseException) e).getErrorCodeAsInt());
                node[0].next(context, nodeListenerFuture);
                authenticationLatch.countDown();
            }
        });

        sleepForFragmentToCommit();

        Intent intent = new Intent();
        intent.putExtra(Fido.FIDO2_KEY_ERROR_EXTRA, Base64.decode(ERROR_FIDO2_KEY_ERROR, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING ));
        getFragment().onActivityResult(WebAuthnHeadlessAuthenticateFragment.REQUEST_FIDO2_SIGNIN,
                0, intent);

        authenticationLatch.await();

        RecordedRequest request = server.takeRequest(); //first Request
        request = server.takeRequest(); //webauthn authenticatio
        String body = request.getBody().readUtf8();
        JSONObject result = new JSONObject(body);
        assertThat(result.getJSONArray("callbacks").length()).isEqualTo(2);
        JSONObject hiddenValueCallback = result.getJSONArray("callbacks").getJSONObject(1);
        assertThat(hiddenValueCallback.getString("type")).isEqualTo("HiddenValueCallback");
        assertThat(hiddenValueCallback.getJSONArray("input").length()).isEqualTo(1);
        String value = hiddenValueCallback.getJSONArray("input").getJSONObject(0).getString("value");
        assertThat(value).isEqualTo("ERROR::SecurityError:The incoming request cannot be validated");

    }

    /**
     * Sleep for a while to let the FragmentManager to commit the Fragment
     */
    private void sleepForFragmentToCommit() throws InterruptedException {
        Thread.sleep(100);
    }

    private WebAuthnHeadlessAuthenticateFragment getFragment() {
        return (WebAuthnHeadlessAuthenticateFragment) activityRule.getActivity().getSupportFragmentManager()
                .findFragmentByTag(WebAuthnHeadlessAuthenticateFragment.TAG);
    }

}
