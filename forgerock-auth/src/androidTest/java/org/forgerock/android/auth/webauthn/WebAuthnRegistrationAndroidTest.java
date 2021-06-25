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
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback;
import org.forgerock.android.auth.exception.WebAuthnResponseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class WebAuthnRegistrationAndroidTest extends WebAuthnTest {

    private static final String SUCCESS_FIDO2_KEY_RESPONSE_EXTRA = "RU___xgCAAACAP__SAAAAEEAAAABK1OPHJfLzC51p88WTVGLwkeU0GsgXwTTOGkav0-RIiGy634kk3gOz2e_r_I-xNFkyn0NiR-lKiqtTjRQzwY7JgAAAAMA___QAAAAygAAAHsidHlwZSI6IndlYmF1dGhuLmNyZWF0ZSIsImNoYWxsZW5nZSI6ImNtSW5EZkE2VUFMTmhJUlhXYzRmellBVzFmR0tBRVZEcVJpSWdUcXpURDQiLCJvcmlnaW4iOiJhbmRyb2lkOmFway1rZXktaGFzaDpSOHhPN3JsUVdhV0w0QmxGeWdwdFdSYjVxY0tXZGZqelpJYVNSaXQ5WFZ3IiwiYW5kcm9pZFBhY2thZ2VOYW1lIjoib3JnLmZvcmdlcm9jay5hdXRoIn0AAAQA___oAAAA4wAAAKNjZm10ZG5vbmVnYXR0U3RtdKBoYXV0aERhdGFYxX2IE7CjrnLcgPrR-U5RSE6T5bC10JWT1E2_u0U97Z4hRQAAAAAAAAAAAAAAAAAAAAAAAAAAAEEBK1OPHJfLzC51p88WTVGLwkeU0GsgXwTTOGkav0-RIiGy634kk3gOz2e_r_I-xNFkyn0NiR-lKiqtTjRQzwY7JqUBAgMmIAEhWCAyD-pOMu97QL0ZJCGt5r93yBGAzgODv8GjrhbXnG5TBiJYIBk2ztjTWIegVnAJy0tkwz_YqmTfiNhXz48M8VGWH0HLAA";
    private static final String ERROR_FIDO2_KEY_ERROR = "RU___2gAAAACAAQAEgAAAAMA__9YAAAAKAAAAFQAaABlACAAaQBuAGMAbwBtAGkAbgBnACAAcgBlAHEAdQBlAHMAdAAgAGMAYQBuAG4AbwB0ACAAYgBlACAAdgBhAGwAaQBkAGEAdABlAGQAAAAAAA";
    private static final String ERROR_FIDO2_UNSUPPORTED = "RU___3AAAAACAAQACQAAAAMA__9gAAAALQAAAEYASQBEAE8AMgAgAEEAUABJACAAaQBzACAAbgBvAHQAIABzAHUAcABwAG8AcgB0AGUAZAAgAG8AbgAgAGQAZQB2AGkAYwBlAHMAIABiAGUAbABvAHcAIABOAAAA";

    @Test
    public void testDerivedRegistrationCallback() throws ExecutionException, InterruptedException {
        enqueue("/webAuthn_registration_71.json", HttpURLConnection.HTTP_OK);
        enqueue("/success.json", HttpURLConnection.HTTP_OK);

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
        FRSession.authenticate(context, "", nodeListenerFuture);
        nodeListenerFuture.get();
    }

    @Test
    public void testWebAuthnRegistrationHappyPath() throws ExecutionException, InterruptedException, JSONException {

        enqueue("/webAuthn_registration_71.json", HttpURLConnection.HTTP_OK);
        enqueue("/success.json", HttpURLConnection.HTTP_OK);
        CallbackFactory.getInstance().register(MockWebAuthnRegistrationCallback.class);

        CountDownLatch latch = new CountDownLatch(1);
        final WebAuthnRegistrationCallback[] callback = {null};
        final Node[] node = {null};

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture() {
            @Override
            public void onCallbackReceived(Node state) {
                    callback[0] = state.getCallback(MockWebAuthnRegistrationCallback.class);
                    node[0] = state;
                    latch.countDown();
            }
        };

        FRSession.authenticate(context, "", nodeListenerFuture);
        latch.await();

        callback[0].register(node[0], new FRListener<Void>() {
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
        getFragment().onActivityResult(WebAuthnHeadlessRegistrationFragment.REQUEST_FIDO2_REGISTER,
                0, intent);


        FRSession session = nodeListenerFuture.get();
        assertThat(session).isNotNull();

        RecordedRequest request = server.takeRequest(); //first Request
        request = server.takeRequest(); //webauthn registration
        String body = request.getBody().readUtf8();
        JSONObject result = new JSONObject(body);
        assertThat(result.getJSONArray("callbacks").length()).isEqualTo(2);
        JSONObject hiddenValueCallback = result.getJSONArray("callbacks").getJSONObject(1);
        assertThat(hiddenValueCallback.getString("type")).isEqualTo("HiddenValueCallback");
        assertThat(hiddenValueCallback.getJSONArray("input").length()).isEqualTo(1);
        String value = hiddenValueCallback.getJSONArray("input").getJSONObject(0).getString("value");
        assertThat(value).isEqualTo("{\"type\":\"webauthn.create\",\"challenge\":\"cmInDfA6UALNhIRXWc4fzYAW1fGKAEVDqRiIgTqzTD4\",\"origin\":\"android:apk-key-hash:R8xO7rlQWaWL4BlFygptWRb5qcKWdfjzZIaSRit9XVw\",\"androidPackageName\":\"org.forgerock.auth\"}::-93,99,102,109,116,100,110,111,110,101,103,97,116,116,83,116,109,116,-96,104,97,117,116,104,68,97,116,97,88,-59,125,-120,19,-80,-93,-82,114,-36,-128,-6,-47,-7,78,81,72,78,-109,-27,-80,-75,-48,-107,-109,-44,77,-65,-69,69,61,-19,-98,33,69,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,65,1,43,83,-113,28,-105,-53,-52,46,117,-89,-49,22,77,81,-117,-62,71,-108,-48,107,32,95,4,-45,56,105,26,-65,79,-111,34,33,-78,-21,126,36,-109,120,14,-49,103,-65,-81,-14,62,-60,-47,100,-54,125,13,-119,31,-91,42,42,-83,78,52,80,-49,6,59,38,-91,1,2,3,38,32,1,33,88,32,50,15,-22,78,50,-17,123,64,-67,25,36,33,-83,-26,-65,119,-56,17,-128,-50,3,-125,-65,-63,-93,-82,22,-41,-100,110,83,6,34,88,32,25,54,-50,-40,-45,88,-121,-96,86,112,9,-53,75,100,-61,63,-40,-86,100,-33,-120,-40,87,-49,-113,12,-15,81,-106,31,65,-53::AStTjxyXy8wudafPFk1Ri8JHlNBrIF8E0zhpGr9PkSIhsut-JJN4Ds9nv6_yPsTRZMp9DYkfpSoqrU40UM8GOyY");
    }

    @Test
    public void testWebAuthnRegistrationError() throws InterruptedException, JSONException {

        enqueue("/webAuthn_registration_71.json", HttpURLConnection.HTTP_OK);
        CallbackFactory.getInstance().register(MockWebAuthnRegistrationCallback.class);

        CountDownLatch latch = new CountDownLatch(1);
        final WebAuthnRegistrationCallback[] callback = {null};
        final Node[] node = {null};

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture() {
            @Override
            public void onCallbackReceived(Node state) {
                callback[0] = state.getCallback(MockWebAuthnRegistrationCallback.class);
                node[0] = state;
                latch.countDown();
            }
        };

        FRSession.authenticate(context, "", nodeListenerFuture);
        latch.await();

        CountDownLatch registerlatch = new CountDownLatch(1);
        callback[0].register(node[0], new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                registerlatch.countDown();
                fail();
            }

            @Override
            public void onException(Exception e) {
                assertThat(e instanceof WebAuthnResponseException).isTrue();
                assertThat(((WebAuthnResponseException) e).getErrorCode()).isEqualTo(ErrorCode.SECURITY_ERR);
                assertThat(((WebAuthnResponseException) e).getErrorCodeAsInt()).isEqualTo(ErrorCode.SECURITY_ERR.getCode());
                node[0].next(context, nodeListenerFuture);
                registerlatch.countDown();
            }
        });

        sleepForFragmentToCommit();

        Intent intent = new Intent();
        intent.putExtra(Fido.FIDO2_KEY_ERROR_EXTRA, Base64.decode(ERROR_FIDO2_KEY_ERROR, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING ));
        getFragment().onActivityResult(WebAuthnHeadlessRegistrationFragment.REQUEST_FIDO2_REGISTER,
                0, intent);

        registerlatch.await();

        RecordedRequest request = server.takeRequest(); //first Request
        request = server.takeRequest(); //webauthn registration
        String body = request.getBody().readUtf8();
        JSONObject result = new JSONObject(body);
        assertThat(result.getJSONArray("callbacks").length()).isEqualTo(2);
        JSONObject hiddenValueCallback = result.getJSONArray("callbacks").getJSONObject(1);
        assertThat(hiddenValueCallback.getString("type")).isEqualTo("HiddenValueCallback");
        assertThat(hiddenValueCallback.getJSONArray("input").length()).isEqualTo(1);
        String value = hiddenValueCallback.getJSONArray("input").getJSONObject(0).getString("value");
        assertThat(value).isEqualTo("ERROR::SecurityError:The incoming request cannot be validated");
    }

    @Test
    public void testWebAuthnRegistrationUnsupported() throws InterruptedException, JSONException {

        enqueue("/webAuthn_registration_71.json", HttpURLConnection.HTTP_OK);
        CallbackFactory.getInstance().register(MockWebAuthnRegistrationCallback.class);

        CountDownLatch latch = new CountDownLatch(1);
        final WebAuthnRegistrationCallback[] callback = {null};
        final Node[] node = {null};

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture() {
            @Override
            public void onCallbackReceived(Node state) {
                callback[0] = state.getCallback(MockWebAuthnRegistrationCallback.class);
                node[0] = state;
                latch.countDown();
            }
        };

        FRSession.authenticate(context, "", nodeListenerFuture);
        latch.await();

        CountDownLatch registerlatch = new CountDownLatch(1);
        callback[0].register(node[0], new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                registerlatch.countDown();
                fail();
            }

            @Override
            public void onException(Exception e) {
                assertThat(e instanceof WebAuthnResponseException).isTrue();
                assertThat(((WebAuthnResponseException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_SUPPORTED_ERR);
                assertThat(((WebAuthnResponseException) e).getErrorCodeAsInt()).isEqualTo(ErrorCode.NOT_SUPPORTED_ERR.getCode());
                node[0].next(context, nodeListenerFuture);
                registerlatch.countDown();
            }
        });

        sleepForFragmentToCommit();

        Intent intent = new Intent();
        intent.putExtra(Fido.FIDO2_KEY_ERROR_EXTRA, Base64.decode(ERROR_FIDO2_UNSUPPORTED, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING ));
        getFragment().onActivityResult(WebAuthnHeadlessRegistrationFragment.REQUEST_FIDO2_REGISTER,
                0, intent);

        registerlatch.await();

        RecordedRequest request = server.takeRequest(); //first Request
        request = server.takeRequest(); //webauthn registration
        String body = request.getBody().readUtf8();
        JSONObject result = new JSONObject(body);
        assertThat(result.getJSONArray("callbacks").length()).isEqualTo(2);
        JSONObject hiddenValueCallback = result.getJSONArray("callbacks").getJSONObject(1);
        assertThat(hiddenValueCallback.getString("type")).isEqualTo("HiddenValueCallback");
        assertThat(hiddenValueCallback.getJSONArray("input").length()).isEqualTo(1);
        String value = hiddenValueCallback.getJSONArray("input").getJSONObject(0).getString("value");
        assertThat(value).isEqualTo("unsupported");
    }


    /**
     * Sleep for a while to let the FragmentManager to commit the Fragment
     */
    private void sleepForFragmentToCommit() throws InterruptedException {
        Thread.sleep(10);
    }

    private WebAuthnHeadlessRegistrationFragment getFragment() {
        return (WebAuthnHeadlessRegistrationFragment) activityRule.getActivity().getSupportFragmentManager()
                .findFragmentByTag(WebAuthnHeadlessRegistrationFragment.TAG);
    }

}
