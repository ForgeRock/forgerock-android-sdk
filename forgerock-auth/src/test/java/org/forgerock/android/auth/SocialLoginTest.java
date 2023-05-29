/*
 * Copyright (c) 2021 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.forgerock.android.auth.callback.CallbackFactory;
import org.forgerock.android.auth.callback.IdPCallback;
import org.forgerock.android.auth.callback.SelectIdPCallback;
import org.forgerock.android.auth.idp.AppleSignInHandler;
import org.forgerock.android.auth.idp.GoogleIdentityServicesHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class SocialLoginTest extends BaseTest {

    private static final String DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest";
    private static final String DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest";

    private GoogleIdentityServicesHandler getGoogleIdentityServicesHandler(FragmentActivity activity) {
        Fragment fragment = activity.getSupportFragmentManager()
                .findFragmentByTag(GoogleIdentityServicesHandler.TAG);
        if (fragment == null) {
            return null;
        }
        return (GoogleIdentityServicesHandler) fragment;
    }

    private AppleSignInHandler getAppleSignInHandler(FragmentActivity activity) {
        Fragment fragment = activity.getSupportFragmentManager()
                .findFragmentByTag(AppleSignInHandler.TAG);
        if (fragment == null) {
            return null;
        }
        return (AppleSignInHandler) fragment;
    }

    @After
    public void resetIdPCallback() {
        CallbackFactory.getInstance().register(IdPCallback.class);
    }

    @Test
    public void testSelectIdPLocalAuthentication() throws ExecutionException, InterruptedException, JSONException {
        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

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

        ActivityScenario scenario = ActivityScenario.launch(DummyActivity.class);
        scenario.onActivity(InitProvider::setCurrentActivity);

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

        idPCallback[0].signIn(null, new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                finalState[0].next(context, nodeListenerFuture);
            }

            @Override
            public void onException(Exception e) {
                fail(e.getMessage());
            }
        });
        Status status = Status.RESULT_SUCCESS;
        Parcel statusParcel = Parcel.obtain();
        status.writeToParcel(statusParcel, 0);
        byte[] bytes = statusParcel.marshall();

        Parcel signInCredentialParcel = Parcel.obtain();
        // NOTE: Upgrading the Google Library to latest breaks this test because,  SignInCredential class is package private now, cannot be used in test . keeping this for reference and applied a workaround.
        // SignInCredential signInCredential = new SignInCredential("1234", "", "", "", null, "", "dummy_id_token", "");
        writeToParcel(signInCredentialParcel, 0, "1234", "dummy_id_token");
        byte[] bytes2 = signInCredentialParcel.marshall();


        Intent intent = new Intent();
        intent.putExtra("sign_in_credential", bytes2);
        intent.putExtra("status", bytes);

        scenario.onActivity(activity -> getGoogleIdentityServicesHandler((FragmentActivity) activity).onActivityResult(GoogleIdentityServicesHandler.RC_SIGN_IN, Activity.RESULT_OK, intent));


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


    private void writeToParcel(@NonNull Parcel dest, int flags, String id, String token) {
        int var10000 = SafeParcelWriter.beginObjectHeader(dest);
        SafeParcelWriter.writeString(dest, 1, id, false);
        SafeParcelWriter.writeString(dest, 2, "", false);
        SafeParcelWriter.writeString(dest, 3,"", false);
        SafeParcelWriter.writeString(dest, 4, "", false);
        SafeParcelWriter.writeParcelable(dest, 5, Uri.EMPTY, flags, false);
        SafeParcelWriter.writeString(dest, 6, "", false);
        SafeParcelWriter.writeString(dest, 7, token, false);
        SafeParcelWriter.writeString(dest, 8, "", false);
        SafeParcelWriter.finishObjectHeader(dest, var10000);
    }


    @Test
    public void testErrorWithGoogle() throws InterruptedException, ExecutionException, JSONException {

        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_IdPCallback.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        //Create a dummy Fragment
        ActivityScenario scenario = ActivityScenario.launch(DummyActivity.class);
        scenario.onActivity(InitProvider::setCurrentActivity);

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
        idPCallback[0].signIn(null, new FRListener<Void>() {
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

        scenario.onActivity(activity -> getGoogleIdentityServicesHandler((FragmentActivity) activity).onActivityResult(GoogleIdentityServicesHandler.RC_SIGN_IN, Activity.RESULT_OK, null));
        countDownLatch.await();
    }

    @Test
    public void testHappyPathWithApple() throws InterruptedException, ExecutionException, JSONException, PackageManager.NameNotFoundException {

        CallbackFactory.getInstance().register(IdPCallbackMock.class);

        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_IdPCallback_apple.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        ActivityScenario scenario = ActivityScenario.launch(DummyActivity.class);
        scenario.onActivity(InitProvider::setCurrentActivity);


        CountDownLatch executeTree = new CountDownLatch(2);

        final IdPCallbackMock[] idPCallback = {null};
        final Node[] finalState = {null};
        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(SelectIdPCallback.class) != null) {
                    state.getCallback(SelectIdPCallback.class).setValue("apple");
                    state.next(context, this);
                    executeTree.countDown();
                    return;
                }

                if (state.getCallback(IdPCallbackMock.class) != null) {
                    //idPCallback.signIn needs to be run in Main thread in order to
                    //launch the Fragment
                    idPCallback[0] = state.getCallback(IdPCallbackMock.class);
                    finalState[0] = state;
                    executeTree.countDown();
                }
            }
        };

        FRSession.authenticate(context, "", nodeListenerFuture);

        //Wait for the idPCallback to finish
        executeTree.await();

        idPCallback[0].signIn(null, new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                finalState[0].next(context, nodeListenerFuture);
            }

            @Override
            public void onException(Exception e) {
                fail(e.getMessage());
            }
        });

        Intent intent = new Intent();
        intent.setData(Uri.parse("https://opeam.example.com?form_post_entry=dummyValue"));
        scenario.onActivity(activity -> getAppleSignInHandler((FragmentActivity) activity).onActivityResult(AppleSignInHandler.RC_SIGN_IN, Activity.RESULT_OK, intent));

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
        assertThat(value).isEqualTo("apple");

        //IdPCallback
        recordedRequest = server.takeRequest(); //Select IdPCallback
        body = recordedRequest.getBody().readUtf8();
        result = new JSONObject(body);
        assertThat(result.getJSONArray("callbacks").length()).isEqualTo(1);
        JSONObject idPCallbackReq = result.getJSONArray("callbacks").getJSONObject(0);
        assertThat(idPCallbackReq.getString("type")).isEqualTo("IdPCallback");
        assertThat(idPCallbackReq.getJSONArray("input").length()).isEqualTo(2);
        String token = idPCallbackReq.getJSONArray("input").getJSONObject(0).getString("value");
        assertThat(token).isEqualTo("form_post_entry");
        String tokenType = idPCallbackReq.getJSONArray("input").getJSONObject(1).getString("value");
        assertThat(tokenType).isEqualTo("authorization_code");
        assertThat(Uri.parse(recordedRequest.getPath()).getQueryParameter("form_post_entry")).isEqualTo("dummyValue");

    }

    @Test
    public void testAppleSignInWithoutNonce() throws InterruptedException, ExecutionException, JSONException {

        CallbackFactory.getInstance().register(IdPCallbackMock.class);

        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_IdPCallback_apple_no_nonce.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());

        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        InitProvider.setCurrentActivity(fragmentActivity);

        CountDownLatch executeTree = new CountDownLatch(2);

        final IdPCallbackMock[] idPCallback = {null};
        final Node[] finalState = {null};
        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(SelectIdPCallback.class) != null) {
                    state.getCallback(SelectIdPCallback.class).setValue("apple");
                    state.next(context, this);
                    executeTree.countDown();
                    return;
                }

                if (state.getCallback(IdPCallbackMock.class) != null) {
                    //idPCallback.signIn needs to be run in Main thread in order to
                    //launch the Fragment
                    idPCallback[0] = state.getCallback(IdPCallbackMock.class);
                    finalState[0] = state;
                    executeTree.countDown();
                }
            }
        };

        FRSession.authenticate(context, "", nodeListenerFuture);

        //Wait for the idPCallback to finish
        executeTree.await();

        final boolean[] result = {false};
        CountDownLatch signInCountDownLatch = new CountDownLatch(1);
        idPCallback[0].signIn(null, new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                fail("Expect fail when no nonce");
            }

            @Override
            public void onException(Exception e) {
                result[0] = true;
                assertThat(e).isInstanceOf(IllegalArgumentException.class);
                signInCountDownLatch.countDown();
            }
        });

        signInCountDownLatch.await();
        assertThat(result[0]).isTrue();
    }


    @Test
    public void testUnsupportedHandler() throws InterruptedException {
        enqueue("/authTreeMockTest_Authenticate_SelectIdPCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_IdPCallback_unsupport_provider.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setUrl(getUrl());

        //Create a dummy Fragment
        FragmentActivity fragmentActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        InitProvider.setCurrentActivity(fragmentActivity);

        NodeListenerFuture<FRSession> nodeListenerFuture = new NodeListenerFuture<FRSession>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(SelectIdPCallback.class) != null) {
                    state.getCallback(SelectIdPCallback.class).setValue("dummy");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(IdPCallback.class) != null) {

                    NodeListenerFuture<FRSession> nodeListener = this;
                    IdPCallback idPCallback = state.getCallback(IdPCallback.class);
                    idPCallback.signIn(null, new FRListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            fail();
                        }

                        @Override
                        public void onException(Exception e) {
                            nodeListener.onException(e);
                        }
                    });
                }
            }
        };

        FRSession.authenticate(context, "", nodeListenerFuture);

        try {
            nodeListenerFuture.get();
            fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
