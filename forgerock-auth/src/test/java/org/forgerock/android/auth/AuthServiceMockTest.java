/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.net.Uri;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.forgerock.android.auth.callback.AbstractPromptCallback;
import org.forgerock.android.auth.callback.CallbackFactory;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.forgerock.android.auth.exception.ApiException;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.exception.AuthenticationTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import javax.security.auth.callback.UnsupportedCallbackException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class AuthServiceMockTest extends BaseTest {

    @Test
    public void authTreeWithOAuth2() throws InterruptedException, ExecutionException, JSONException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);


        final AuthService authService = AuthService.builder()
                .serverConfig(serverConfig)
                .name("Example")
                .build();

        final List<Node> nodes = new ArrayList<>();
        NodeListenerFuture<SSOToken> nodeListenerFuture = new NodeListenerFuture<SSOToken>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    nodes.add(state);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                    nodes.add(state);
                }
            }
        };

        authService.next(context, nodeListenerFuture);
        OAuth2TokenListenerFuture oAuth2TokenListenerFuture = new OAuth2TokenListenerFuture();
        oAuth2Client.exchangeToken(nodeListenerFuture.get(), emptyMap(), oAuth2TokenListenerFuture);

        RecordedRequest recordedRequest = server.takeRequest();
        //Assert OAuth Token
        assertNotNull(oAuth2TokenListenerFuture.get());
        AccessToken accessToken = oAuth2TokenListenerFuture.get();
        assertNotNull(accessToken.getValue());
        assertNotNull(accessToken.getRefreshToken());
        assertNotNull(accessToken.getIdToken());
        assertEquals(3, accessToken.getScope().size());
        assertTrue(accessToken.getScope().contains("openid"));
        assertTrue(accessToken.getScope().contains("email"));
        assertTrue(accessToken.getScope().contains("address"));
        assertEquals("Bearer", accessToken.getTokenType());
        assertEquals(3599, accessToken.getExpiresIn());

        assertEquals("/json/realms/root/authenticate?authIndexType=service&authIndexValue=Example", recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
        recordedRequest = server.takeRequest();
        assertEquals("/json/realms/root/authenticate", recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
        recordedRequest = server.takeRequest();
        assertEquals("/json/realms/root/authenticate", recordedRequest.getPath());
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals(nodes.get(1).toJsonObject().toString(), recordedRequest.getBody().readUtf8());
        recordedRequest = server.takeRequest();
        Uri uri = Uri.parse(recordedRequest.getPath());
        assertEquals("/oauth2/realms/root/authorize", uri.getPath());
        assertEquals(nodeListenerFuture.get().getValue(), recordedRequest.getHeader("iPlanetDirectoryPro"));
        assertEquals("andy_app", uri.getQueryParameter("client_id"));
        assertEquals("openid email address", uri.getQueryParameter("scope"));
        assertEquals("code", uri.getQueryParameter("response_type"));
        assertEquals("https://www.example.com:8080/callback", uri.getQueryParameter("redirect_uri"));
        assertNotNull(uri.getQueryParameter("code_challenge"));
        assertEquals("S256", uri.getQueryParameter("code_challenge_method"));

        assertEquals("GET", recordedRequest.getMethod());
        //assertEquals("scope=write&state=abc123&client_id=andy_app&csrf=C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*&response_type=code&redirect_uri=http%3A%2F%2Fwww.example.com%3A8080%2Fcallback&decision=allow&code_challenge=IpSeJZQ9QOUL0TIn3rX_eZTYiq-zOXgaaZQBUX8G-I4&code_challenge_method=S256"
        //        , recordedRequest.getBody().readString(Charset.defaultCharset());
    }

    @Test
    public void authTreeCallbackTest() throws InterruptedException, ExecutionException, JSONException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        final AuthService authService = AuthService.builder()
                .serverConfig(serverConfig)
                .name("Example")
                .build();

        final List<Node> nodes = new ArrayList<>();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    nodes.add(state);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    PasswordCallback passwordCallback = state.getCallback(PasswordCallback.class);
                    passwordCallback.setPassword("password".toCharArray());
                    state.setCallback(passwordCallback);
                    state.next(context, this);
                    nodes.add(state);
                }
            }
        };

        authService.next(context, nodeListenerFuture);

        server.takeRequest(); //next
        RecordedRequest rr = server.takeRequest(); //Username Post
        assertEquals(nodes.get(0).toJsonObject().toString(), rr.getBody().readUtf8());
        rr = server.takeRequest(); //Password Post
        assertEquals(nodes.get(1).toJsonObject().toString(), rr.getBody().readUtf8());

    }

    @Test(expected = AuthenticationException.class)
    public void authTreeWithAuthFailed() throws Throwable {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_fail.json", HttpURLConnection.HTTP_UNAUTHORIZED);

        final AuthService authService = AuthService.builder()
                .serverConfig(serverConfig)
                .name("Example")
                .build();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("invalid".toCharArray());
                    state.next(context, this);
                }
            }
        };

        authService.next(context, nodeListenerFuture);
        try {
            nodeListenerFuture.get();
        } catch (ExecutionException e) {
            AuthenticationException authenticationException = (AuthenticationException) e.getCause();
            assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, authenticationException.getStatusCode());
            assertEquals(getJson("/authTreeMockTest_Authenticate_fail.json"), authenticationException.getMessage());
            throw e.getCause();
        }
    }

    @Test
    public void authTreeWithAuthFailedThenResubmit() throws Throwable {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_fail.json", HttpURLConnection.HTTP_UNAUTHORIZED);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        final AuthService authService = AuthService.builder()
                .serverConfig(serverConfig)
                .name("Example")
                .build();

        final Node[] lastNode = {null};
        NodeListenerFuture<Token> nodeListenerFuture = new NodeListenerFuture<Token>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(NameCallback.class) != null) {
                    state.getCallback(NameCallback.class).setName("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(PasswordCallback.class) != null) {
                    state.getCallback(PasswordCallback.class).setPassword("invalid".toCharArray());
                    state.next(context, this);
                    lastNode[0] = state;
                }
            }
        };

        authService.next(context, nodeListenerFuture);
        try {
            nodeListenerFuture.get();
            fail();
        } catch (ExecutionException e) {
            AuthenticationException authenticationException = (AuthenticationException) e.getCause();
            assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, authenticationException.getStatusCode());
            assertEquals(getJson("/authTreeMockTest_Authenticate_fail.json"), authenticationException.getMessage());
        }

        nodeListenerFuture.reset();
        lastNode[0].next(context, nodeListenerFuture);
        Token token = nodeListenerFuture.get();
        assertNotNull(token);

        server.takeRequest(); //next
        server.takeRequest(); //Username Post
        server.takeRequest(); //Password Post
        RecordedRequest recordedRequest = server.takeRequest(); //Password Post again
        //Assert that it re-posts the last state
        assertEquals(lastNode[0].toJsonObject().toString(), recordedRequest.getBody().readUtf8());

    }

    @Test
    public void authTreeWithInvalidAuthIdSignature() throws InterruptedException {

        String responseBody = "{\n" +
                "    \"code\": 400,\n" +
                "    \"reason\": \"Bad Request\",\n" +
                "    \"message\": \"Authentication Error: Invalid AuthId Signature\"\n" +
                "}";

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse().setBody(responseBody).setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST));

        final AuthService authService = AuthService.builder()
                .serverConfig(serverConfig)
                .name("Example")
                .build();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                state.getCallback(NameCallback.class).setName("tester");
                state.next(context, this);
            }
        };

        authService.next(context, nodeListenerFuture);
        try {
            nodeListenerFuture.get();
            fail();
        } catch (ExecutionException e) {
            ApiException apiException = (ApiException) e.getCause();
            assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, apiException.getStatusCode());
            assertEquals(responseBody, apiException.getMessage());
        }
    }

    @Test
    public void authTreeWithSessionTimeout() throws InterruptedException {

        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_SessionTimeout.json", HttpURLConnection.HTTP_UNAUTHORIZED);

        final AuthService authService = AuthService.builder()
                .serverConfig(serverConfig)
                .name("Example")
                .build();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                state.getCallback(NameCallback.class).setName("tester");
                state.next(context, this);
            }
        };

        authService.next(context, nodeListenerFuture);
        try {
            nodeListenerFuture.get();
            fail();
        } catch (ExecutionException e) {
            AuthenticationTimeoutException cause = (AuthenticationTimeoutException) e.getCause();
            assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, cause.getStatusCode());
            assertEquals(getJson("/authTreeMockTest_Authenticate_SessionTimeout.json"), cause.getMessage());
        }
    }

    @Test
    public void authTreePageCallbackTest() throws InterruptedException, ExecutionException, JSONException {

        enqueue("/authTreeMockTest_Authenticate_PageCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        final AuthService authService = AuthService.builder()
                .serverConfig(serverConfig)
                .name("Example")
                .build();

        final List<Node> nodes = new ArrayList<>();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
                state.getCallback(NameCallback.class).setName("tester");

                //Different way to set Callback
                PasswordCallback passwordCallback = state.getCallback(PasswordCallback.class);
                passwordCallback.setPassword("password".toCharArray());
                state.setCallback(passwordCallback);
                state.next(context, this);
                nodes.add(state);
            }
        };

        authService.next(context, nodeListenerFuture);

        server.takeRequest(); //next
        RecordedRequest rr = server.takeRequest(); //PageCallback Post
        String body = rr.getBody().readUtf8();

        //Assert What received
        assertEquals("UsernamePassword", nodes.get(0).getStage());
        assertEquals("HeaderValue", nodes.get(0).getHeader());
        assertEquals("PageDesc", nodes.get(0).getDescription());
        assertEquals(2, nodes.get(0).getCallbacks().size());

        //Assert what sent to server
        assertEquals(nodes.get(0).toJsonObject().toString(), body);
        JSONObject jsonBody = new JSONObject(body);
        assertEquals("UsernamePassword", jsonBody.getString("stage"));
        assertEquals("tester", ((JSONObject) ((JSONObject) jsonBody.getJSONArray("callbacks").get(0))
                .getJSONArray("input").get(0)).getString("value"));
        assertEquals("password", ((JSONObject) ((JSONObject) jsonBody.getJSONArray("callbacks").get(1))
                .getJSONArray("input").get(0)).getString("value"));
        assertEquals("UsernamePassword", jsonBody.getString("stage"));

    }

    @Test(expected = IllegalStateException.class)
    public void authTreeInvalidState() {

        Node node = new Node("", "","", "", "dummy", null);
        NodeListenerFuture future = new NodeListenerFuture() {
            @Override
            public void onCallbackReceived(Node node) {

            }
        };
        node.next(context, future);
    }


    @Test
    public void customCallback() throws ExecutionException, InterruptedException {

        enqueue("/authTreeMockTest_Authenticate_MyCustomCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        CallbackFactory.getInstance().register(MyCustomCallback.class);

        final AuthService authService = AuthService.builder()
                .serverConfig(serverConfig)
                .name("Example")
                .build();

        NodeListenerFuture<Token> nodeListenerFuture = new NodeListenerFuture<Token>() {

            @Override
            public void onCallbackReceived(Node state) {
                state.getCallback(MyCustomCallback.class).setCustomField("test");
                state.next(context, this);
            }
        };

        authService.next(context, nodeListenerFuture);
        Token token = nodeListenerFuture.get();
        assertNotNull(token);

    }

    @Test
    public void customGlobalCallback() throws ExecutionException, InterruptedException {

        enqueue("/authTreeMockTest_Authenticate_MyCustomCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        CallbackFactory.getInstance().register(MyCustomCallback.class);

        final AuthService authService = AuthService.builder()
                .serverConfig(serverConfig)
                .name("Example")
                .build();

        NodeListenerFuture<Token> nodeListenerFuture = new NodeListenerFuture<Token>() {

            @Override
            public void onCallbackReceived(Node state) {
                state.getCallback(MyCustomCallback.class).setCustomField("test");
                state.next(context, this);
            }
        };

        authService.next(context, nodeListenerFuture);
        Token token = nodeListenerFuture.get();
        assertNotNull(token);

    }


    @Test(expected = UnsupportedCallbackException.class)
    public void UnSupportedCustomCallback() throws Throwable {

        enqueue("/authTreeMockTest_Authenticate_UnsupportedCustomCallback.json", HttpURLConnection.HTTP_OK);

        final AuthService authService = AuthService.builder()
                .serverConfig(serverConfig)
                .name("Example")
                .build();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node state) {
            }
        };

        authService.next(context, nodeListenerFuture);
        try {
            nodeListenerFuture.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }

    }

    @Test
    public void authTreeMetadataWithStageCallbackTest() throws InterruptedException, JSONException {

        enqueue("/authTreeMockTest_Authenticate_MetadataWithStageCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);

        final AuthService authService = AuthService.builder()
                .serverConfig(serverConfig)
                .name("Example")
                .build();

        final List<Node> nodes = new ArrayList<>();

        NodeListenerFuture nodeListenerFuture = new NodeListenerFuture() {

            @Override
            public void onCallbackReceived(Node node) {
                node.getCallback(NameCallback.class).setName("tester");

                //Different way to set Callback
                PasswordCallback passwordCallback = node.getCallback(PasswordCallback.class);
                passwordCallback.setPassword("password".toCharArray());
                node.setCallback(passwordCallback);
                node.next(context, this);
                nodes.add(node);
            }
        };

        authService.next(context, nodeListenerFuture);

        server.takeRequest(); //next
        RecordedRequest rr = server.takeRequest(); //PageCallback Post
        String body = rr.getBody().readUtf8();

        //Assert What received
        assertEquals("UsernamePassword", nodes.get(0).getStage());
        assertEquals(3, nodes.get(0).getCallbacks().size());

        //Assert what sent to server
        assertEquals(nodes.get(0).toJsonObject().toString(), body);
        JSONObject jsonBody = new JSONObject(body);
        assertEquals("tester", ((JSONObject) ((JSONObject) jsonBody.getJSONArray("callbacks").get(0))
                .getJSONArray("input").get(0)).getString("value"));
        assertEquals("password", ((JSONObject) ((JSONObject) jsonBody.getJSONArray("callbacks").get(1))
                .getJSONArray("input").get(0)).getString("value"));
        assertEquals("UsernamePassword", ((JSONObject) ((JSONObject) jsonBody.getJSONArray("callbacks").get(2))
                .getJSONArray("output").get(0)).getJSONObject("value").getString("stage"));
        assertEquals("UsernamePassword", jsonBody.getString("stage"));

    }

    public static final class MyCustomCallback extends AbstractPromptCallback {

        public MyCustomCallback() {
            super();
        }

        public MyCustomCallback(JSONObject jsonObject, int index) {
            super(jsonObject, index);
        }

        public void setCustomField(String value) {
            setValue(value);
        }

        @Override
        public String getType() {
            return "MyCustomCallback";
        }
    }

}
