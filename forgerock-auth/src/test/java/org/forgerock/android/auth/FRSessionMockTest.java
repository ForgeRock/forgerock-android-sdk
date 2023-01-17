/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.net.Uri;
import android.os.OperationCanceledException;
import android.util.Pair;

import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.callback.SuspendedTextOutputCallback;
import org.forgerock.android.auth.exception.SuspendedAuthSessionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.forgerock.android.auth.Action.AUTHENTICATE;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class FRSessionMockTest extends BaseTest {

    private static final String DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest";


    @After
    public void closeSession() throws Exception {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout();
        }
    }

    @Test
    public void frSessionHappyPath() throws InterruptedException, ExecutionException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setUrl(getUrl());
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }
            }
        };

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        Assert.assertTrue(nodeListenerFuture.get() instanceof FRSession);
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());
    }

    @Test
    public void frSessionWithPolicyAdvice() throws InterruptedException, ExecutionException {

        //First Round
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        //Second Round
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setUrl(getUrl());
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }
            }
        };

        PolicyAdvice advice = PolicyAdvice.builder()
                .type("TransactionConditionAdvice")
                .value("3b8c1b2b-0aed-461a-a49b-f35da8276d12").build();

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        Assert.assertTrue(nodeListenerFuture.get() instanceof FRSession);
        Assert.assertNotNull(FRSession.getCurrentSession());
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());

        nodeListenerFuture.reset();
        FRSession.getCurrentSession().authenticate(context, advice, nodeListenerFuture);
        Assert.assertTrue(nodeListenerFuture.get() instanceof FRSession);

        server.takeRequest();
        server.takeRequest();
        server.takeRequest();

        RecordedRequest recordedRequest = server.takeRequest(); //The one with step up
        Uri uri = Uri.parse(recordedRequest.getPath());
        assertThat(uri.getQueryParameter("authIndexType")).isEqualTo("composite_advice");
        assertThat(uri.getQueryParameter("authIndexValue")).isEqualTo(advice.toString());

        //Make sure we have sent the request.
        server.takeRequest();
        server.takeRequest();


    }

    @Test
    public void testFRSessionReAuthenticate() throws Exception {

        //First Round
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        //Second Round
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setUrl(getUrl());
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }
            }
        };

        FRSession.authenticate(context, "Example", nodeListenerFuture);

        Assert.assertTrue(nodeListenerFuture.get() instanceof FRSession);

        nodeListenerFuture.reset();

        FRSession.authenticate(context, "Example", nodeListenerFuture);

        Assert.assertTrue(nodeListenerFuture.get() instanceof FRSession);

    }

    @Test
    public void testWithNoSession() throws ExecutionException, InterruptedException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success_withNoSession.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setUrl(getUrl());
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }
            }
        };

        RequestInterceptorRegistry.getInstance().register((FRRequestInterceptor<Action>) (request, tag) -> {
            if (tag.getType().equals(AUTHENTICATE)) {
                return request.newBuilder()
                        .url(Uri.parse(request.url().toString())
                                .buildUpon()
                                .appendQueryParameter("noSession", "true").toString())
                        .build();
            }
            return request;
        });

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        assertThat(nodeListenerFuture.get()).isNull();
        assertThat(FRSession.getCurrentSession()).isNull();
        assertThat(FRUser.getCurrentUser()).isNull();

        RecordedRequest recordedRequest = server.takeRequest(); //NameCallback
        recordedRequest = server.takeRequest(); //PasswordCallback without Session
        assertThat(Uri.parse(recordedRequest.getPath()).getQueryParameter("noSession")).isEqualTo("true");
    }

    @Test
    public void testWithSessionThenWithoutSession() throws ExecutionException, InterruptedException {
        frSessionHappyPath();

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success_withNoSession.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setUrl(getUrl());

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }
            }
        };

        RequestInterceptorRegistry.getInstance().register((FRRequestInterceptor<Action>) (request, tag) -> {
            if (tag.getType().equals(AUTHENTICATE)) {
                return request.newBuilder()
                        .url(Uri.parse(request.url().toString())
                                .buildUpon()
                                .appendQueryParameter("noSession", "true").toString())
                        .build();
            }
            return request;
        });

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        assertThat(nodeListenerFuture.get()).isNull(); //The without session oen should return null
        //Retrieve the previous session
        Assert.assertNotNull(FRSession.getCurrentSession()); //Retrieve the previous Session
        Assert.assertNotNull(FRSession.getCurrentSession().getSessionToken());

        RecordedRequest recordedRequest = server.takeRequest(); //NameCallback with Session
        recordedRequest = server.takeRequest(); //PasswordCallback with Session
        recordedRequest = server.takeRequest(); //End of tree with Session

        recordedRequest = server.takeRequest(); //NameCallback without Session
        recordedRequest = server.takeRequest(); //PasswordCallback without Session
        recordedRequest = server.takeRequest(); //PasswordCallback without Session
        assertThat(Uri.parse(recordedRequest.getPath()).getQueryParameter("noSession")).isEqualTo("true");

    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoSuspendId() {
        FRSession.authenticate(context, Uri.parse("http://dummy/"), null);
    }

    @Test
    public void testWithSuspendedEmail() throws ExecutionException, InterruptedException {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_EmailSuspended.json", HttpURLConnection.HTTP_OK);

        Config.getInstance().setUrl(getUrl());

        final boolean[] suspended = {false};

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }

                if (state.getCallback(SuspendedTextOutputCallback.class) != null) {
                    suspended[0] = true;
                    this.onException(new OperationCanceledException());
                }

            }
        };

        FRSession.authenticate(context, "Example", nodeListenerFuture);
        try {
            nodeListenerFuture.get();
            fail("Should throw exception");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(OperationCanceledException.class);
        }
        assertThat(FRSession.getCurrentSession()).isNull();
        assertThat(suspended[0]).isTrue();

    }

    @Test
    public void testWithResumeUri() throws ExecutionException, InterruptedException {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        final HashMap<String, Pair<Action, Integer>> result = new HashMap<>();
        RequestInterceptorRegistry.getInstance().register(request -> {
            String action = ((Action)request.tag()).getType();
            Pair<Action, Integer> pair = result.get(action);
            if ( pair == null) {
                result.put(action, new Pair<>((Action) request.tag(), 1));
            } else {
                result.put(action, new Pair<>((Action) request.tag(), pair.second + 1));
            }
            return request;
        });

        Config.getInstance().setUrl(getUrl());
        Config.getInstance().setSsoSharedPreferences(context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                }

            }
        };

        FRSession.authenticate(context,
                Uri.parse("http://openam.example.com:8081/openam/XUI?realm=/&suspendedId=YGJ1o1snV96U6u7XT8SaHhX4Cv8"),
                nodeListenerFuture);


        assertThat(nodeListenerFuture.get()).isInstanceOf(FRSession.class);
        assertThat(result.get("RESUME_AUTHENTICATE")).isNotNull();
        assertThat(FRSession.getCurrentSession()).isNotNull();
        assertThat(FRSession.getCurrentSession().getSessionToken()).isNotNull();
        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/json/realms/root/authenticate?suspendedId=YGJ1o1snV96U6u7XT8SaHhX4Cv8");
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");

    }

    @Test
    public void testWithExpiredSuspendedId() throws InterruptedException {
        enqueue("/authTreeMockTest_Authenticate_Expired_SuspendedId.json", HttpURLConnection.HTTP_UNAUTHORIZED);

        Config.getInstance().setUrl(getUrl());
        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
            }
        };

        FRSession.authenticate(context,
                Uri.parse("http://openam.example.com:8081/openam/XUI?realm=/&suspendedId=YGJ1o1snV96U6u7XT8SaHhX4Cv8"),
                nodeListenerFuture);


        try {
            nodeListenerFuture.get();
            fail("Should throw exception");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(SuspendedAuthSessionException.class);
        }
    }

    @Test
    public void testLogout() throws ExecutionException, InterruptedException {

        frSessionHappyPath();
        enqueue("/sessions_logout.json", HttpURLConnection.HTTP_OK);

        FRSession.getCurrentSession().logout();

        //Check SSOToken Storage
        final SingleSignOnManager singleSignOnManager = DefaultSingleSignOnManager.builder()
                .context(context)
                .build();

        assertThat(singleSignOnManager.getToken()).isNull();

        assertThat(FRSession.getCurrentSession()).isNull();

    }
}
