/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.okhttp.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.*
import org.forgerock.android.auth.callback.BooleanAttributeInputCallback
import org.forgerock.android.auth.callback.KbaCreateCallback
import org.forgerock.android.auth.callback.NumberAttributeInputCallback
import org.forgerock.android.auth.callback.StringAttributeInputCallback
import org.forgerock.android.auth.callback.TermsAndConditionsCallback
import org.forgerock.android.auth.callback.ValidatedPasswordCallback
import org.forgerock.android.auth.callback.ValidatedUsernameCallback
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.util.concurrent.ExecutionException

@RunWith(AndroidJUnit4::class)
class FRAuthRegistrationMockTest : BaseTest() {
    /**
     * Start -> Platform Username -> Platform Password -> Attribute Collector -> Create Object
     */
    @Test
    @Throws(InterruptedException::class, ExecutionException::class, JSONException::class)
    fun frAuthRegistrationHappyPath() {
        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)
        Config.getInstance().sharedPreferences = context.getSharedPreferences(
            DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE)
        Config.getInstance().ssoSharedPreferences =
            context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST,
                Context.MODE_PRIVATE)
        Config.getInstance().url = url

        val nodeListenerFuture: NodeListenerFuture<FRUser> =
            object : NodeListenerFuture<FRUser>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback(ValidatedUsernameCallback::class.java) != null) {
                        state.getCallback(ValidatedUsernameCallback::class.java)
                            .setUsername("tester")
                        state.next(context, this)
                        return
                    }
                    if (state.getCallback(ValidatedPasswordCallback::class.java) != null) {
                        state.getCallback(ValidatedPasswordCallback::class.java)
                            .setPassword("password".toCharArray())
                        state.next(context, this)
                        return
                    }
                    val callbacks = state.callbacks
                    val email = callbacks[0] as StringAttributeInputCallback
                    val firstName = callbacks[1] as StringAttributeInputCallback
                    val lastName = callbacks[2] as StringAttributeInputCallback
                    val age = callbacks[3] as NumberAttributeInputCallback
                    val happy = callbacks[4] as BooleanAttributeInputCallback
                    assertEquals("mail", email.name)
                    assertEquals("Email Address", email.prompt)
                    assertTrue(email.isRequired)
                    assertEquals("valid-email-address-format", email.policies
                        .getJSONArray("policies")
                        .getJSONObject(0).optString("policyId"))
                    assertEquals("maximum-length", email.policies
                        .getJSONArray("policies")
                        .getJSONObject(1).optString("policyId"))
                    assertEquals(0, email.failedPolicies.size.toLong())
                    assertEquals("", email.value)
                    assertTrue(email.validateOnly)
                    email.value = "test@test.com"
                    assertEquals("givenName", firstName.name)
                    assertEquals("First Name", firstName.prompt)
                    assertTrue(firstName.isRequired)
                    assertEquals("minimum-length", firstName.policies
                        .getJSONArray("policies")
                        .getJSONObject(0).optString("policyId"))
                    assertEquals("maximum-length", firstName.policies
                        .getJSONArray("policies")
                        .getJSONObject(1).optString("policyId"))
                    assertFalse(firstName.validateOnly)
                    assertEquals(0, firstName.failedPolicies.size.toLong())
                    assertEquals("", firstName.value)
                    firstName.value = "My First Name"
                    assertEquals("sn", lastName.name)
                    assertEquals("Last Name", lastName.prompt)
                    assertTrue(lastName.isRequired)
                    assertEquals("minimum-length", lastName.policies
                        .getJSONArray("policies")
                        .getJSONObject(0).optString("policyId"))
                    assertEquals("maximum-length", lastName.policies
                        .getJSONArray("policies")
                        .getJSONObject(1).optString("policyId"))
                    assertEquals(0, lastName.failedPolicies.size.toLong())
                    assertEquals("", lastName.value)
                    assertFalse(lastName.validateOnly)
                    lastName.value = "My Last Name"
                    assertThat(happy.name).isEqualTo("happy")
                    assertThat(happy.prompt).isEqualTo("Happy")
                    assertThat(happy.isRequired).isTrue
                    assertThat(happy.policies.getString("name")).isEqualTo("happy")
                    assertThat(happy.failedPolicies).isEmpty()
                    assertThat(happy.validateOnly).isFalse
                    assertThat(happy.value).isFalse
                    happy.value = true
                    assertThat(age.name).isEqualTo("age")
                    assertThat(age.prompt).isEqualTo("Age")
                    assertThat(age.isRequired).isTrue
                    assertThat(age.policies.getString("name")).isEqualTo("age")
                    assertThat(age.failedPolicies).isEmpty()
                    assertThat(age.validateOnly).isFalse
                    assertNull(age.value)
                    age.value = 30.0
                    state.next(context, this)
                }
            }
        FRUser.register(context, nodeListenerFuture)
        server.takeRequest() //start
        server.takeRequest() //Platform Username
        server.takeRequest() //Password Collector
        var request = server.takeRequest() //Attribute Collector
        val body = JSONObject(request.body.readUtf8())
        assertEquals("test@test.com", body.getJSONArray("callbacks")
            .getJSONObject(0)
            .getJSONArray("input")
            .getJSONObject(0).getString("value"))
        assertEquals("My First Name", body.getJSONArray("callbacks")
            .getJSONObject(1)
            .getJSONArray("input")
            .getJSONObject(0).getString("value"))
        assertEquals("My Last Name", body.getJSONArray("callbacks")
            .getJSONObject(2)
            .getJSONArray("input")
            .getJSONObject(0).getString("value"))
        assertThat(body.getJSONArray("callbacks")
            .getJSONObject(3)
            .getJSONArray("input")
            .getJSONObject(0).getDouble("value")).isEqualTo(30.0)
        assertThat(body.getJSONArray("callbacks")
            .getJSONObject(4)
            .getJSONArray("input")
            .getJSONObject(0).getBoolean("value")).isTrue
        request = server.takeRequest()
        val state = Uri.parse(request.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        assertNotNull(nodeListenerFuture.get())
    }

    /**
     * Start -> Platform Username (UNIQUE) -> Platform Password -> Attribute Collector -> Create Object
     */
    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun frAuthRegistrationWithConstraintViolation() {
        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_platform_username_unique.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)
        Config.getInstance().sharedPreferences = context.getSharedPreferences(
            DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE)
        Config.getInstance().ssoSharedPreferences =
            context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST,
                Context.MODE_PRIVATE)
        Config.getInstance().url = url
        val unique = booleanArrayOf(false)
        val nodeListenerFuture: NodeListenerFuture<FRUser> =
            object : NodeListenerFuture<FRUser>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback(ValidatedUsernameCallback::class.java) != null) {
                        val callback = state.getCallback(
                            ValidatedUsernameCallback::class.java)
                        if (unique[0]) {
                            assertEquals("Username", callback.prompt)
                            try {
                                assertEquals("unique", callback.policies
                                    .getJSONArray("policies")
                                    .getJSONObject(0).optString("policyId"))
                                assertEquals("no-internal-user-conflict", callback.policies
                                    .getJSONArray("policies")
                                    .getJSONObject(1).optString("policyId"))
                                assertEquals("cannot-contain-characters", callback.policies
                                    .getJSONArray("policies")
                                    .getJSONObject(2).optString("policyId"))
                                assertEquals("minimum-length", callback.policies
                                    .getJSONArray("policies")
                                    .getJSONObject(3).optString("policyId"))
                                assertEquals("maximum-length", callback.policies
                                    .getJSONArray("policies")
                                    .getJSONObject(4).optString("policyId"))
                                assertEquals(1, callback.failedPolicies.size.toLong())
                                assertEquals("UNIQUE",
                                    callback.failedPolicies[0].policyRequirement)
                                state.getCallback(ValidatedUsernameCallback::class.java)
                                    .setUsername("tester")
                                state.next(context, this)
                            } catch (e: JSONException) {
                                throw RuntimeException(e)
                            }
                            return
                        }
                        unique[0] = true
                        callback.setUsername("tester")
                        state.next(context, this)
                        return
                    }
                    if (state.getCallback(ValidatedPasswordCallback::class.java) != null) {
                        state.getCallback(ValidatedPasswordCallback::class.java)
                            .setPassword("password".toCharArray())
                        state.next(context, this)
                        return
                    }
                    val callbacks = state.callbacks
                    val email = callbacks[0] as StringAttributeInputCallback
                    val firstName = callbacks[1] as StringAttributeInputCallback
                    val lastName = callbacks[2] as StringAttributeInputCallback
                    email.value = "test@test.com"
                    firstName.value = "My First Name"
                    lastName.value = "My Last Name"
                    state.next(context, this)
                }
            }
        FRUser.register(context, nodeListenerFuture)
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        val request = server.takeRequest()
        val state = Uri.parse(request.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        assertNotNull(nodeListenerFuture.get())
    }

    /**
     * Start -> Platform Username (MIN_LENGTH) -> Platform Password -> Attribute Collector -> Create Object
     */
    @Test
    @Throws(InterruptedException::class, ExecutionException::class)
    fun frAuthRegistrationWithMinLength() {
        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_platform_username_minLength.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)
        Config.getInstance().sharedPreferences = context.getSharedPreferences(
            DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE)
        Config.getInstance().ssoSharedPreferences =
            context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST,
                Context.MODE_PRIVATE)
        Config.getInstance().url = url
        val minLength = booleanArrayOf(false)
        val nodeListenerFuture: NodeListenerFuture<FRUser> =
            object : NodeListenerFuture<FRUser>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback(ValidatedUsernameCallback::class.java) != null) {
                        val callback = state.getCallback(
                            ValidatedUsernameCallback::class.java)
                        if (minLength[0]) {
                            assertEquals("Username", callback.prompt)
                            try {
                                assertEquals("unique", callback.policies
                                    .getJSONArray("policies")
                                    .getJSONObject(0).optString("policyId"))
                                assertEquals("no-internal-user-conflict", callback.policies
                                    .getJSONArray("policies")
                                    .getJSONObject(1).optString("policyId"))
                                assertEquals("cannot-contain-characters", callback.policies
                                    .getJSONArray("policies")
                                    .getJSONObject(2).optString("policyId"))
                                assertEquals("minimum-length", callback.policies
                                    .getJSONArray("policies")
                                    .getJSONObject(3).optString("policyId"))
                                assertEquals("maximum-length", callback.policies
                                    .getJSONArray("policies")
                                    .getJSONObject(4).optString("policyId"))
                                assertEquals(1, callback.failedPolicies.size.toLong())
                                assertEquals("MIN_LENGTH",
                                    callback.failedPolicies[0].policyRequirement)
                                assertEquals(3,
                                    callback.failedPolicies[0].params["minLength"])
                                state.getCallback(ValidatedUsernameCallback::class.java)
                                    .setUsername("tester")
                                state.next(context, this)
                            } catch (e: JSONException) {
                                throw RuntimeException(e)
                            }
                            return
                        }
                        minLength[0] = true
                        callback.setUsername("tester")
                        state.next(context, this)
                        return
                    }
                    if (state.getCallback(ValidatedPasswordCallback::class.java) != null) {
                        state.getCallback(ValidatedPasswordCallback::class.java)
                            .setPassword("password".toCharArray())
                        state.next(context, this)
                        return
                    }
                    val callbacks = state.callbacks
                    val email = callbacks[0] as StringAttributeInputCallback
                    val firstName = callbacks[1] as StringAttributeInputCallback
                    val lastName = callbacks[2] as StringAttributeInputCallback
                    email.value = "test@test.com"
                    firstName.value = "My First Name"
                    lastName.value = "My Last Name"
                    state.next(context, this)
                }
            }
        FRUser.register(context, nodeListenerFuture)
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        server.takeRequest()
        val request = server.takeRequest()
        val state = Uri.parse(request.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        assertNotNull(nodeListenerFuture.get())
    }

    /**
     * Start -> Platform Username -> Attribute Collector -> Platform Password -> KBA Definition -> Create Object
     */
    @Test
    @Throws(InterruptedException::class, ExecutionException::class, JSONException::class)
    fun frAuthRegistrationWithKBA() {
        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_kba_definition.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)
        Config.getInstance().sharedPreferences = context.getSharedPreferences(
            DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE)
        Config.getInstance().ssoSharedPreferences =
            context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST,
                Context.MODE_PRIVATE)
        Config.getInstance().url = url
        val nodeListenerFuture: NodeListenerFuture<FRUser> =
            object : NodeListenerFuture<FRUser>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback(ValidatedUsernameCallback::class.java) != null) {
                        state.getCallback(ValidatedUsernameCallback::class.java)
                            .setUsername("tester")
                        state.next(context, this)
                        return
                    }
                    if (state.getCallback(ValidatedPasswordCallback::class.java) != null) {
                        state.getCallback(ValidatedPasswordCallback::class.java)
                            .setPassword("password".toCharArray())
                        state.next(context, this)
                        return
                    }
                    if (state.getCallback(StringAttributeInputCallback::class.java) != null) {
                        val callbacks = state.callbacks
                        val email = callbacks[0] as StringAttributeInputCallback
                        val firstName = callbacks[1] as StringAttributeInputCallback
                        val lastName = callbacks[2] as StringAttributeInputCallback
                        email.value = "test@test.com"
                        firstName.value = "My First Name"
                        lastName.value = "My Last Name"
                        state.next(context, this)
                        return
                    }
                    val callbacks = state.callbacks
                    val kbaCallback1 = callbacks[0] as KbaCreateCallback
                    val kbaCallback2 = callbacks[1] as KbaCreateCallback
                    kbaCallback1.setSelectedQuestion("What's your favorite color?")
                    kbaCallback1.setSelectedAnswer("Black")
                    kbaCallback2.setSelectedQuestion("Who was your first employer?")
                    kbaCallback2.setSelectedAnswer("Test")
                    state.next(context, this)
                }
            }
        FRUser.register(context, nodeListenerFuture)
        server.takeRequest() //start
        server.takeRequest() //Platform Username
        server.takeRequest() //Attribute Collector
        server.takeRequest() //Password Collector
        var request = server.takeRequest() //KBA Definition
        val body = JSONObject(request.body.readUtf8())

        //First question
        assertEquals("What's your favorite color?", body.getJSONArray("callbacks")
            .getJSONObject(0)
            .getJSONArray("input")
            .getJSONObject(0).getString("value"))
        assertEquals("Black", body.getJSONArray("callbacks")
            .getJSONObject(0)
            .getJSONArray("input")
            .getJSONObject(1).getString("value"))


        //Second question
        assertEquals("Who was your first employer?", body.getJSONArray("callbacks")
            .getJSONObject(1)
            .getJSONArray("input")
            .getJSONObject(0).getString("value"))
        assertEquals("Test", body.getJSONArray("callbacks")
            .getJSONObject(1)
            .getJSONArray("input")
            .getJSONObject(1).getString("value"))
        request = server.takeRequest()
        val state = Uri.parse(request.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        assertNotNull(nodeListenerFuture.get())
    }

    /**
     * Start -> Platform Username -> Attribute Collector -> Platform Password -> KBA Definition -> Create Object
     */
    @Test
    @Throws(InterruptedException::class, ExecutionException::class, JSONException::class)
    fun frAuthRegistrationWithTermsAndCondition() {
        enqueue("/registration_platform_username.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_attribute_collector.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_platform_password.json", HttpURLConnection.HTTP_OK)
        enqueue("/registration_accept_terms_and_conditions.json", HttpURLConnection.HTTP_OK)
        enqueue("/authTreeMockTest_Authenticate_success.json", HttpURLConnection.HTTP_OK)
        Config.getInstance().sharedPreferences = context.getSharedPreferences(
            DEFAULT_TOKEN_MANAGER_TEST, Context.MODE_PRIVATE)
        Config.getInstance().ssoSharedPreferences =
            context.getSharedPreferences(DEFAULT_SSO_TOKEN_MANAGER_TEST,
                Context.MODE_PRIVATE)
        Config.getInstance().url = url
        val nodeListenerFuture: NodeListenerFuture<FRUser> =
            object : NodeListenerFuture<FRUser>() {
                override fun onCallbackReceived(state: Node) {
                    if (state.getCallback(ValidatedUsernameCallback::class.java) != null) {
                        state.getCallback(ValidatedUsernameCallback::class.java)
                            .setUsername("tester")
                        state.next(context, this)
                        return
                    }
                    if (state.getCallback(ValidatedPasswordCallback::class.java) != null) {
                        state.getCallback(ValidatedPasswordCallback::class.java)
                            .setPassword("password".toCharArray())
                        state.next(context, this)
                        return
                    }
                    if (state.getCallback(StringAttributeInputCallback::class.java) != null) {
                        val callbacks = state.callbacks
                        val email = callbacks[0] as StringAttributeInputCallback
                        val firstName = callbacks[1] as StringAttributeInputCallback
                        val lastName = callbacks[2] as StringAttributeInputCallback
                        email.value = "test@test.com"
                        firstName.value = "My First Name"
                        lastName.value = "My Last Name"
                        state.next(context, this)
                        return
                    }
                    val termsAndConditionsCallback = state.getCallback(
                        TermsAndConditionsCallback::class.java)
                    assertEquals("1.0", termsAndConditionsCallback.version)
                    assertEquals("This is a demo for Terms & Conditions",
                        termsAndConditionsCallback.terms)
                    assertEquals("2019-07-11T22:23:55.737Z",
                        termsAndConditionsCallback.createDate)
                    termsAndConditionsCallback.setAccept(true)
                    state.next(context, this)
                }
            }
        FRUser.register(context, nodeListenerFuture)
        server.takeRequest() //start
        server.takeRequest() //Platform Username
        server.takeRequest() //Attribute Collector
        server.takeRequest() //Password Collector
        var request = server.takeRequest() //Terms and Conditions
        val body = JSONObject(request.body.readUtf8())

        //First question
        assertTrue(body.getJSONArray("callbacks")
            .getJSONObject(0)
            .getJSONArray("input")
            .getJSONObject(0).getBoolean("value"))
        request = server.takeRequest()
        val state = Uri.parse(request.path).getQueryParameter("state")
        server.enqueue(MockResponse()
            .addHeader("Location",
                "http://www.example.com:8080/callback?code=PmxwECH3mBobKuPEtPmq6Xorgzo&iss=http://openam.example.com:8080/openam/oauth2&" +
                        "state=" + state + "&client_id=andy_app")
            .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP))
        enqueue("/authTreeMockTest_Authenticate_accessToken.json", HttpURLConnection.HTTP_OK)
        assertNotNull(nodeListenerFuture.get())
    }

    companion object {
        private const val DEFAULT_TOKEN_MANAGER_TEST = "DefaultTokenManagerTest"
        private const val DEFAULT_SSO_TOKEN_MANAGER_TEST = "DefaultSSOManagerTest"
    }
}