/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import org.forgerock.android.auth.callback.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class FRAuthRegistrationMockTest extends BaseTest {

    private static final String DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest";

    /**
     * Start -> Platform Username -> Platform Password -> Attribute Collector -> Create Object
     */
    @Test
    public void frAuthRegistrationHappyPath() throws InterruptedException, ExecutionException, JSONException {

        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(ValidatedCreateUsernameCallback.class) != null) {
                    state.getCallback(ValidatedCreateUsernameCallback.class).setUsername("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(ValidatedCreatePasswordCallback.class) != null) {
                    state.getCallback(ValidatedCreatePasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                    return;
                }

                List<Callback> callbacks = state.getCallbacks();
                StringAttributeInputCallback email = ((StringAttributeInputCallback) callbacks.get(0));
                StringAttributeInputCallback firstName = ((StringAttributeInputCallback) callbacks.get(1));
                StringAttributeInputCallback lastName = ((StringAttributeInputCallback) callbacks.get(2));

                assertEquals("mail", email.getName());
                assertEquals("Email Address", email.getPrompt());
                assertTrue(email.isRequired());
                assertEquals("valid-email-address-format", email.getPolicies().get(0));
                assertEquals("maximum-length", email.getPolicies().get(1));
                assertEquals(0, email.getFailedPolicies().size());
                assertEquals("", email.getValue());
                email.setValue("test@test.com");

                assertEquals("givenName", firstName.getName());
                assertEquals("First Name", firstName.getPrompt());
                assertTrue(firstName.isRequired());
                assertEquals("minimum-length", firstName.getPolicies().get(0));
                assertEquals("maximum-length", firstName.getPolicies().get(1));
                assertEquals(0, firstName.getFailedPolicies().size());
                assertEquals("", firstName.getValue());
                firstName.setValue("My First Name");

                assertEquals("sn", lastName.getName());
                assertEquals("Last Name", lastName.getPrompt());
                assertTrue(lastName.isRequired());
                assertEquals("minimum-length", lastName.getPolicies().get(0));
                assertEquals("maximum-length", lastName.getPolicies().get(1));
                assertEquals(0, lastName.getFailedPolicies().size());
                assertEquals("", lastName.getValue());
                lastName.setValue("My Last Name");

                state.next(context, this);

            }
        };

        FRUser.register(context, nodeListenerFuture);

        assertNotNull(nodeListenerFuture.get());

        server.takeRequest(); //start
        server.takeRequest(); //Platform Username
        server.takeRequest(); //Password Collector
        RecordedRequest request = server.takeRequest(); //Attribute Collector

        JSONObject body = new JSONObject(request.getBody().readUtf8());

        Assert.assertEquals("test@test.com", body.getJSONArray("callbacks")
                .getJSONObject(0)
                .getJSONArray("input")
                .getJSONObject(0).getString("value"));

        Assert.assertEquals("My First Name", body.getJSONArray("callbacks")
                .getJSONObject(1)
                .getJSONArray("input")
                .getJSONObject(0).getString("value"));

        Assert.assertEquals("My Last Name", body.getJSONArray("callbacks")
                .getJSONObject(2)
                .getJSONArray("input")
                .getJSONObject(0).getString("value"));


    }

    /**
     * Start -> Platform Username (UNIQUE) -> Platform Password -> Attribute Collector -> Create Object
     */
    @Test
    public void frAuthRegistrationWithConstraintViolation() throws InterruptedException, ExecutionException {

        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_platform_username_unique.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

        final boolean[] unique = {false};
        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(ValidatedCreateUsernameCallback.class) != null) {
                    ValidatedCreateUsernameCallback callback = state.getCallback(ValidatedCreateUsernameCallback.class);
                    if (unique[0]) {
                        assertEquals("Username", callback.getPrompt());
                        assertEquals("unique", callback.getPolicies().get(0));
                        assertEquals("no-internal-user-conflict", callback.getPolicies().get(1));
                        assertEquals("cannot-contain-characters", callback.getPolicies().get(2));
                        assertEquals("minimum-length", callback.getPolicies().get(3));
                        assertEquals("maximum-length", callback.getPolicies().get(4));
                        assertEquals(1, callback.getFailedPolicies().size());
                        assertEquals("UNIQUE", callback.getFailedPolicies().get(0).getPolicyRequirement());
                        state.getCallback(ValidatedCreateUsernameCallback.class).setUsername("tester");
                        state.next(context, this);
                        return;
                    }
                    unique[0] = true;
                    callback.setUsername("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(ValidatedCreatePasswordCallback.class) != null) {
                    state.getCallback(ValidatedCreatePasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                    return;
                }

                List<Callback> callbacks = state.getCallbacks();
                StringAttributeInputCallback email = ((StringAttributeInputCallback) callbacks.get(0));
                StringAttributeInputCallback firstName = ((StringAttributeInputCallback) callbacks.get(1));
                StringAttributeInputCallback lastName = ((StringAttributeInputCallback) callbacks.get(2));

                email.setValue("test@test.com");
                firstName.setValue("My First Name");
                lastName.setValue("My Last Name");

                state.next(context, this);

            }
        };

        FRUser.register(context, nodeListenerFuture);

        assertNotNull(nodeListenerFuture.get());

    }

    /**
     * Start -> Platform Username (MIN_LENGTH) -> Platform Password -> Attribute Collector -> Create Object
     */
    @Test
    public void frAuthRegistrationWithMinLength() throws InterruptedException, ExecutionException {

        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_platform_username_minLength.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

        final boolean[] minLength = {false};
        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(ValidatedCreateUsernameCallback.class) != null) {
                    ValidatedCreateUsernameCallback callback = state.getCallback(ValidatedCreateUsernameCallback.class);
                    if (minLength[0]) {
                        assertEquals("Username", callback.getPrompt());
                        assertEquals("unique", callback.getPolicies().get(0));
                        assertEquals("no-internal-user-conflict", callback.getPolicies().get(1));
                        assertEquals("cannot-contain-characters", callback.getPolicies().get(2));
                        assertEquals("minimum-length", callback.getPolicies().get(3));
                        assertEquals("maximum-length", callback.getPolicies().get(4));
                        assertEquals(1, callback.getFailedPolicies().size());
                        assertEquals("MIN_LENGTH", callback.getFailedPolicies().get(0).getPolicyRequirement());
                        assertEquals(3, callback.getFailedPolicies().get(0).getParams().get("minLength"));
                        state.getCallback(ValidatedCreateUsernameCallback.class).setUsername("tester");
                        state.next(context, this);
                        return;
                    }
                    minLength[0] = true;
                    callback.setUsername("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(ValidatedCreatePasswordCallback.class) != null) {
                    state.getCallback(ValidatedCreatePasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                    return;
                }

                List<Callback> callbacks = state.getCallbacks();
                StringAttributeInputCallback email = ((StringAttributeInputCallback) callbacks.get(0));
                StringAttributeInputCallback firstName = ((StringAttributeInputCallback) callbacks.get(1));
                StringAttributeInputCallback lastName = ((StringAttributeInputCallback) callbacks.get(2));

                email.setValue("test@test.com");
                firstName.setValue("My First Name");
                lastName.setValue("My Last Name");

                state.next(context, this);

            }
        };

        FRUser.register(context, nodeListenerFuture);

        assertNotNull(nodeListenerFuture.get());

    }

    /**
     * Start -> Platform Username -> Attribute Collector -> Platform Password -> KBA Definition -> Create Object
     */
    @Test
    public void frAuthRegistrationWithKBA() throws InterruptedException, ExecutionException, JSONException {

        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_kba_definition.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(ValidatedCreateUsernameCallback.class) != null) {
                    state.getCallback(ValidatedCreateUsernameCallback.class).setUsername("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(ValidatedCreatePasswordCallback.class) != null) {
                    state.getCallback(ValidatedCreatePasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                    return;
                }


                if (state.getCallback(StringAttributeInputCallback.class) != null) {
                    List<Callback> callbacks = state.getCallbacks();
                    StringAttributeInputCallback email = ((StringAttributeInputCallback) callbacks.get(0));
                    StringAttributeInputCallback firstName = ((StringAttributeInputCallback) callbacks.get(1));
                    StringAttributeInputCallback lastName = ((StringAttributeInputCallback) callbacks.get(2));

                    email.setValue("test@test.com");
                    firstName.setValue("My First Name");
                    lastName.setValue("My Last Name");

                    state.next(context, this);
                    return;
                }

                List<Callback> callbacks = state.getCallbacks();
                KbaCreateCallback kbaCallback1 = ((KbaCreateCallback) callbacks.get(0));
                KbaCreateCallback kbaCallback2 = ((KbaCreateCallback) callbacks.get(1));

                kbaCallback1.setSelectedQuestion("What's your favorite color?");
                kbaCallback1.setSelectedAnswer("Black");

                kbaCallback2.setSelectedQuestion("Who was your first employer?");
                kbaCallback2.setSelectedAnswer("Test");
                state.next(context, this);
            }
        };

        FRUser.register(context, nodeListenerFuture);

        assertNotNull(nodeListenerFuture.get());
        server.takeRequest(); //start
        server.takeRequest(); //Platform Username
        server.takeRequest(); //Attribute Collector
        server.takeRequest(); //Password Collector
        RecordedRequest request = server.takeRequest(); //KBA Definition

        JSONObject body = new JSONObject(request.getBody().readUtf8());

        //First question
        Assert.assertEquals("What's your favorite color?", body.getJSONArray("callbacks")
                .getJSONObject(0)
                .getJSONArray("input")
                .getJSONObject(0).getString("value"));
        Assert.assertEquals("Black", body.getJSONArray("callbacks")
                .getJSONObject(0)
                .getJSONArray("input")
                .getJSONObject(1).getString("value"));


        //Second question
        Assert.assertEquals("Who was your first employer?", body.getJSONArray("callbacks")
                .getJSONObject(1)
                .getJSONArray("input")
                .getJSONObject(0).getString("value"));
        Assert.assertEquals("Test", body.getJSONArray("callbacks")
                .getJSONObject(1)
                .getJSONArray("input")
                .getJSONObject(1).getString("value"));


    }

    /**
     * Start -> Platform Username -> Attribute Collector -> Platform Password -> KBA Definition -> Create Object
     */
    @Test
    public void frAuthRegistrationWithTermsAndCondition() throws InterruptedException, ExecutionException, JSONException {

        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK);
        enqueue("/registration_accept_terms_and_conditions.json", HttpURLConnection.HTTP_OK);
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK);
        server.enqueue(new MockResponse()
                .addHeader("Location", "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&state=abc123&client_id=andy_app")
                .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP));
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK);

        Config.getInstance(context).setSharedPreferences(context.getSharedPreferences(DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE));
        Config.getInstance(context).setUrl(getUrl());
        Config.getInstance(context).setEncryptor(new MockEncryptor());

        NodeListenerFuture<FRUser> nodeListenerFuture = new NodeListenerFuture<FRUser>() {

            @Override
            public void onCallbackReceived(Node state) {
                if (state.getCallback(ValidatedCreateUsernameCallback.class) != null) {
                    state.getCallback(ValidatedCreateUsernameCallback.class).setUsername("tester");
                    state.next(context, this);
                    return;
                }

                if (state.getCallback(ValidatedCreatePasswordCallback.class) != null) {
                    state.getCallback(ValidatedCreatePasswordCallback.class).setPassword("password".toCharArray());
                    state.next(context, this);
                    return;
                }


                if (state.getCallback(StringAttributeInputCallback.class) != null) {
                    List<Callback> callbacks = state.getCallbacks();
                    StringAttributeInputCallback email = ((StringAttributeInputCallback) callbacks.get(0));
                    StringAttributeInputCallback firstName = ((StringAttributeInputCallback) callbacks.get(1));
                    StringAttributeInputCallback lastName = ((StringAttributeInputCallback) callbacks.get(2));

                    email.setValue("test@test.com");
                    firstName.setValue("My First Name");
                    lastName.setValue("My Last Name");

                    state.next(context, this);
                    return;
                }

                TermsAndConditionsCallback termsAndConditionsCallback = state.getCallback(TermsAndConditionsCallback.class);
                assertEquals("1.0", termsAndConditionsCallback.getVersion() );
                assertEquals("This is a demo for Terms & Conditions", termsAndConditionsCallback.getTerms() );
                assertEquals("2019-07-11T22:23:55.737Z", termsAndConditionsCallback.getCreateDate() );
                termsAndConditionsCallback.setAccept(true);

                state.next(context, this);
            }
        };

        FRUser.register(context, nodeListenerFuture);

        Assert.assertNotNull(nodeListenerFuture.get());
        server.takeRequest(); //start
        server.takeRequest(); //Platform Username
        server.takeRequest(); //Attribute Collector
        server.takeRequest(); //Password Collector
        RecordedRequest request = server.takeRequest(); //Terms and Conditions

        JSONObject body = new JSONObject(request.getBody().readUtf8());

        //First question
        Assert.assertTrue(body.getJSONArray("callbacks")
                .getJSONObject(0)
                .getJSONArray("input")
                .getJSONObject(0).getBoolean("value"));

    }


}
