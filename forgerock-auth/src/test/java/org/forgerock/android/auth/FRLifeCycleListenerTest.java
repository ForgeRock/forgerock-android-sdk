/*
 * Copyright (c) 2021 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.net.Uri;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.assertj.core.api.Assertions;
import org.forgerock.android.auth.callback.NameCallback;
import org.forgerock.android.auth.callback.PasswordCallback;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

@RunWith(RobolectricTestRunner.class)
public class FRLifeCycleListenerTest extends BaseTest{

    private static final String DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest";

    @Test
    public void testListener() throws InterruptedException, ExecutionException, MalformedURLException, ParseException, JSONException {

        final int[] onSSOTokenUpdated = {0};
        final int[] onCookiesUpdated = {0};
        final int[] onLogout = {0};
        FRLifecycleListener lifecycleListener = new FRLifecycleListener() {
            @Override
            public void onSSOTokenUpdated(SSOToken ssoToken) {
                Assertions.assertThat(ssoToken.getValue()).isEqualTo("C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*");
                onSSOTokenUpdated[0]++;
            }

            @Override
            public void onCookiesUpdated(Collection<String> cookies) {
                onCookiesUpdated[0]++;
            }

            @Override
            public void onLogout() {
                onLogout[0]++;
            }
        };

        FRLifecycle.registerFRLifeCycleListener(lifecycleListener);

        authenticate();

        FRLifecycle.unregisterFRLifeCycleListener(lifecycleListener);

        //Assert that FRLifeCycleListener is invoked
        Assertions.assertThat(onSSOTokenUpdated[0]).isEqualTo(1);
        Assertions.assertThat(onCookiesUpdated[0]).isEqualTo(1);
        Assertions.assertThat(onLogout[0]).isEqualTo(1);


    }

    @Test
    public void testUnRegister() throws InterruptedException, ExecutionException, MalformedURLException, ParseException, JSONException {

        final int[] onSSOTokenUpdated = {0};
        final int[] onCookiesUpdated = {0};
        final int[] onLogout = {0};
        FRLifecycleListener lifecycleListener = new FRLifecycleListener() {
            @Override
            public void onSSOTokenUpdated(SSOToken ssoToken) {
                Assertions.assertThat(ssoToken.getValue()).isEqualTo("C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*");
                onSSOTokenUpdated[0]++;
            }

            @Override
            public void onCookiesUpdated(Collection<String> cookies) {
                onCookiesUpdated[0]++;
            }

            @Override
            public void onLogout() {
                onLogout[0]++;
            }
        };

        FRLifecycle.registerFRLifeCycleListener(lifecycleListener);
        FRLifecycle.unregisterFRLifeCycleListener(lifecycleListener);
        authenticate();

        //Assert that FRLifeCycleListener is invoked
        Assertions.assertThat(onSSOTokenUpdated[0]).isEqualTo(0);
        Assertions.assertThat(onCookiesUpdated[0]).isEqualTo(0);
        Assertions.assertThat(onLogout[0]).isEqualTo(0);


    }

    private void authenticate() throws ExecutionException, InterruptedException {
        enqueue("/authTreeMockTest_Authenticate_NameCallback.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_PasswordCallback.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "iPlanetDirectoryPro=C4VbQPUtfu76IvO_JRYbqtGt2hc.*AAJTSQACMDEAAlNLABxQQ1U3VXZXQ0FoTUNCSnFjbzRYeWh4WHYzK0E9AAR0eXBlAANDVFMAAlMxAAA.*; Path=/; Domain=localhost; HttpOnly")
                .setBody(getJson("/authTreeMockTest_Authenticate_success.json")));

        Config.getInstance().setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance().setUrl(getUrl());
        Config.getInstance().setEncryptor(new MockEncryptor());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

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

        FRUser.login(context, nodeListenerFuture);
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest request = server.takeRequest();
        String state = Uri.parse(request.getPath()).getQueryParameter("state");
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);
        enqueue("/userinfo_success.json", HttpURLConnection.HTTP_OK);


        assertNotNull(nodeListenerFuture.get());
        assertNotNull(FRUser.getCurrentUser());

        FRUser.getCurrentUser().logout();

    }


}
