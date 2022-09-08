/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.net.Uri;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.exception.ApiException;
import org.forgerock.android.auth.exception.AuthorizeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public class OAuth2MockTest extends BaseTest {

    @Test
    public void oAuth2Success() throws ExecutionException, InterruptedException {


        SSOToken token = new SSOToken("ssoToken");
        OAuth2TokenListenerFuture oAuth2TokenListenerFuture = new OAuth2TokenListenerFuture();
        oAuth2Client.exchangeToken(token, emptyMap(), oAuth2TokenListenerFuture);
        RecordedRequest recordedRequest = server.takeRequest(); //authorize
        Assertions.assertThat(recordedRequest.getHeader(serverConfig.getCookieName())).isEqualTo(token.getValue());
        String state = Uri.parse(recordedRequest.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state="+ state +"&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

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

        /*
        RecordedRequest recordedRequest = server.takeRequest(); //authorize
         */

    }

    @Test
    public void oAuth2FailedOnInvalidRedirect() {

        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST));

        SSOToken token = new SSOToken("ssoToken");
        OAuth2TokenListenerFuture oAuth2TokenListenerFuture = new OAuth2TokenListenerFuture();
        oAuth2Client.exchangeToken(token, emptyMap(), oAuth2TokenListenerFuture);

        try {
            assertNotNull(oAuth2TokenListenerFuture.get());
            fail();
        } catch (ExecutionException e) {
            AuthorizeException authorizeException = (AuthorizeException) e.getCause();
            assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, ((ApiException)authorizeException.getCause()).getStatusCode());
        } catch (InterruptedException e) {
            fail();
        }
    }

    @Test
    public void oAuth2FailedOnInvalidSession() throws InterruptedException {

        SSOToken token = new SSOToken("ssoToken");
        OAuth2TokenListenerFuture oAuth2TokenListenerFuture = new OAuth2TokenListenerFuture();
        oAuth2Client.exchangeToken(token, emptyMap(), oAuth2TokenListenerFuture);

        RecordedRequest recordedRequest = server.takeRequest(); //authorize
        String state = Uri.parse(recordedRequest.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?error_description=Failed%20to%20get%20resource%20owner%20session%20from%20request&" +
                        "state="+ state +"&error=invalid_request")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));

        try {
            assertNotNull(oAuth2TokenListenerFuture.get());
            fail();
        } catch (ExecutionException e) {
            AuthorizeException authorizeException = (AuthorizeException) e.getCause();
            assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, ((ApiException)authorizeException.getCause()).getStatusCode());
            assertEquals("Failed to get resource owner session from request", ((ApiException)authorizeException.getCause()).getMessage());
        } catch (InterruptedException e) {
            fail();
        }
    }

    @Test
    public void oAuth2FailedWithInvalidAuthCode() throws InterruptedException {

        String errorMessage = "{\n" +
                "    \"error_description\": \"The provided access grant is invalid, expired, or revoked.\",\n" +
                "    \"error\": \"invalid_grant\"\n" +
                "}";


        SSOToken token = new SSOToken("ssoToken");
        OAuth2TokenListenerFuture oAuth2TokenListenerFuture = new OAuth2TokenListenerFuture();
        oAuth2Client.exchangeToken(token, emptyMap(), oAuth2TokenListenerFuture);

        RecordedRequest recordedRequest = server.takeRequest(); //authorize
        String state = Uri.parse(recordedRequest.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state="+ state +"&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .setBody(errorMessage)
        );



        try {
            oAuth2TokenListenerFuture.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof ApiException);
            assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, ((ApiException)e.getCause()).getStatusCode());
            assertEquals(errorMessage, e.getCause().getMessage());
        } catch (InterruptedException e) {
            fail();
        }
    }

    @Test
    public void oAuth2InvalidState() {

        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        SSOToken token = new SSOToken("ssoToken");
        OAuth2TokenListenerFuture oAuth2TokenListenerFuture = new OAuth2TokenListenerFuture();
        oAuth2Client.exchangeToken(token, emptyMap(), oAuth2TokenListenerFuture);

        try {
            oAuth2TokenListenerFuture.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof AuthorizeException);
            assertTrue(e.getCause().getCause() instanceof IllegalStateException);
        } catch (InterruptedException e) {
            fail();
        }

    }


}
