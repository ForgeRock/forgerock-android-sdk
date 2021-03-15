/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.forgerock.android.auth.callback.IdPCallback;
import org.forgerock.android.auth.callback.SelectIdPCallback;
import org.forgerock.android.auth.idp.GoogleSignInHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class SocialLoginTest extends BaseTest {

    private static final String DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest";
    private static final String DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest";

    private GoogleSignInHandler getGoogleSignInHandler(FragmentActivity activity) {
        Fragment fragment = activity.getSupportFragmentManager()
                .findFragmentByTag(GoogleSignInHandler.TAG);
        if (fragment == null) {
            return null;
        }
        return (GoogleSignInHandler) fragment;
    }

    @Test
    public void testSelectIdPLocalAuthentication() throws ExecutionException, InterruptedException, JSONException {
        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());
        Config.getInstance().setEncryptor(new MockEncryptor());

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(SelectIdPCallback.class) != null) {
                    state.getCallback(SelectIdPCallback.class).setValue("localAuthentication");
                    state.next(context, this);
                }
            }
        };

        FRSession.authenticate(context, "", nodeListenerFuture);

        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        RecordedRequest recordedRequest = server.takeRequest(); //First request
        //SelectIdPCallback
        recordedRequest = server.takeRequest();
        String body = recordedRequest.getBody().readUtf8();
        JSONObject result = new JSONObject(body);
        assertThat(result.getJSONArray("callbacks").length()).isEqualTo(1);
        JSONObject selectIdPCallback = result.getJSONArray("callbacks").getJSONObject(0);
        assertThat(selectIdPCallback.getString("type")).isEqualTo("SelectIdPCallback");
        assertThat(selectIdPCallback.getJSONArray("input").length()).isEqualTo(1);
        String value = selectIdPCallback.getJSONArray("input").getJSONObject(0).getString("value");
        assertThat(value).isEqualTo("localAuthentication");

    }

    @Test
    public void testHappyPathWithGoogle() throws InterruptedException, ExecutionException, JSONException {

        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_IdPCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());
        Config.getInstance().setEncryptor(new MockEncryptor());

        //Create a dummy Fragment
        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        InitProvider.setCurrentActivity(fragmentActivity);

        CountDownLatch executeTree = new CountDownLatch(2);

        final IdPCallback[] idPCallback = {null};
        final Node[] finalState = {null};
        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(SelectIdPCallback.class) != null) {
                    state.getCallback(SelectIdPCallback.class).setValue("google");
                    state.next(context, this);
                    executeTree.countDown();
                    return;
                }

                if (state.getCallback(IdPCallback.class) != null) {
                    //idPCallback.signIn needs to be run in Main thread in order to
                    //launch the Fragment
                    executeTree.countDown();
                    idPCallback[0] = state.getCallback(IdPCallback.class);
                    finalState[0] = state;
                }
            }
        };

        FRSession.authenticate(context, "", nodeListenerFuture);

        //Wait for the idPCallback to finish
        executeTree.await();

        idPCallback[0].signIn(new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                finalState[0].next(context, nodeListenerFuture);
            }

            @Override
            public void onException(Exception e) {
                fail(e.getMessage());
            }
        });

        GoogleSignInAccount googleSignInAccount = null;
        try {
            googleSignInAccount = GoogleSignInAccount.zaa("{\n" +
                    "  \"tokenId\" : \"dummy_id_token\",\n" +
                    "  \"expirationTime\" : 1615838032659,\n" +
                    "  \"grantedScopes\" : [\"email\"],\n" +
                    "  \"obfuscatedIdentifier\": \"1234567\"\n" +
                    "}");
        } catch (JSONException e) {
            fail(e.getMessage());
        }

        Intent intent = new Intent();
        intent.putExtra("googleSignInAccount", googleSignInAccount);
        getGoogleSignInHandler(fragmentActivity).onActivityResult(GoogleSignInHandler.RC_SIGN_IN, Activity.RESULT_OK, intent);

        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        RecordedRequest recordedRequest = server.takeRequest(); //First request
        //SelectIdPCallback
        recordedRequest = server.takeRequest();
        String body = recordedRequest.getBody().readUtf8();
        JSONObject result = new JSONObject(body);
        assertThat(result.getJSONArray("callbacks").length()).isEqualTo(1);
        JSONObject selectIdPCallback = result.getJSONArray("callbacks").getJSONObject(0);
        assertThat(selectIdPCallback.getString("type")).isEqualTo("SelectIdPCallback");
        assertThat(selectIdPCallback.getJSONArray("input").length()).isEqualTo(1);
        String value = selectIdPCallback.getJSONArray("input").getJSONObject(0).getString("value");
        assertThat(value).isEqualTo("google");

        //IdPCallback
        recordedRequest = server.takeRequest(); //Select IdPCallback
        body = recordedRequest.getBody().readUtf8();
        result = new JSONObject(body);
        assertThat(result.getJSONArray("callbacks").length()).isEqualTo(1);
        JSONObject idPCallbackReq = result.getJSONArray("callbacks").getJSONObject(0);
        assertThat(idPCallbackReq.getString("type")).isEqualTo("IdPCallback");
        assertThat(idPCallbackReq.getJSONArray("input").length()).isEqualTo(2);
        String token = idPCallbackReq.getJSONArray("input").getJSONObject(0).getString("value");
        assertThat(token).isEqualTo("dummy_id_token");
        String tokenType = idPCallbackReq.getJSONArray("input").getJSONObject(1).getString("value");
        assertThat(tokenType).isEqualTo("id_token");

    }

    @Test
    public void testErrorWithGoogle() throws InterruptedException, ExecutionException, JSONException {

        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_IdPCallback.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());
        Config.getInstance().setEncryptor(new MockEncryptor());

        //Create a dummy Fragment
        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        InitProvider.setCurrentActivity(fragmentActivity);

        CountDownLatch executeTree = new CountDownLatch(2);

        final IdPCallback[] idPCallback = {null};
        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(SelectIdPCallback.class) != null) {
                    state.getCallback(SelectIdPCallback.class).setValue("google");
                    state.next(context, this);
                    executeTree.countDown();
                    return;
                }

                if (state.getCallback(IdPCallback.class) != null) {
                    //idPCallback.signIn needs to be run in Main thread in order to
                    //launch the Fragment
                    executeTree.countDown();
                    idPCallback[0] = state.getCallback(IdPCallback.class);
                }
            }
        };

        FRSession.authenticate(context, "", nodeListenerFuture);

        //Wait for the idPCallback to finish
        executeTree.await();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        idPCallback[0].signIn(new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                countDownLatch.countDown();
                fail();
            }

            @Override
            public void onException(Exception e) {
                assertThat(e).isInstanceOf(ApiException.class);
                countDownLatch.countDown();
            }
        });

        getGoogleSignInHandler(fragmentActivity).onActivityResult(GoogleSignInHandler.RC_SIGN_IN, Activity.RESULT_OK, null);
        countDownLatch.await();
    }
}
