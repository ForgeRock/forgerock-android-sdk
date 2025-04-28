/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.NodeListener
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

/**
 * Success e2e tests for the [ReCaptchaEnterpriseCallback]
 */
class ReCaptchaEnterpriseCallbackSuccessTest : ReCaptchaEnterpriseCallbackBaseTest() {

    @Test
    fun testRecaptchaEnterpriseSuccess() = runTest {
        val nodeCaptchaHit = intArrayOf(0)
        val nodeTextOutputHit = intArrayOf(0)

        val nodeListenerFuture = object : RecaptchaEnterpriseNodeListener(
            context, "success"
        ) {
            val nodeListener: NodeListener<FRSession?> = this
            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(ReCaptchaEnterpriseCallback::class.java) != null) {
                    val callback = node.getCallback(ReCaptchaEnterpriseCallback::class.java)
                    nodeCaptchaHit[0]++

                    // Make sure that the ReCaptcha site key is set correctly
                    Assert.assertNotNull(callback.reCaptchaSiteKey)
                    Assertions.assertThat(callback.reCaptchaSiteKey).isEqualTo(RECAPTCHA_SITE_KEY)

                    // Execute recaptcha - this should pass...
                    runBlocking {
                        callback.execute(application = application)
                        node.next(context, nodeListener)
                    }
                }
                if (node.getCallback(TextOutputCallback::class.java) != null) {
                    // Make sure that the journey finishes with success
                    nodeTextOutputHit[0]++

                    // Note: The journey sends back to us the content of CaptchaEnterpriseNode.ASSESSMENT_RESULT
                    // in the message of a TextOutputCallback... so we can parse it here and verify results...
                    val message = node.getCallback(TextOutputCallback::class.java).message
                    val jsonObject = JSONObject(message)

                    Assert.assertNotNull(jsonObject)
                    Logger.debug("RecaptchaEnterpriseCallbackTest", jsonObject.toString(2))

                    val name = jsonObject.getString("name")
                    val token = jsonObject.getJSONObject("event").getString("token")
                    val siteKey = jsonObject.getJSONObject("event").getString("siteKey")
                    val userAgent = jsonObject.getJSONObject("event").getString("userAgent")
                    val userIpAddress = jsonObject.getJSONObject("event").getString("userIpAddress")
                    val score = jsonObject.getJSONObject("riskAnalysis").getDouble("score")
                    val valid = jsonObject.getJSONObject("tokenProperties").getBoolean("valid")
                    val action = jsonObject.getJSONObject("tokenProperties").getString("action")
                    val androidPackageName =
                        jsonObject.getJSONObject("tokenProperties").getString("androidPackageName")

                    // Ensure that the name is "recaptcha"
                    Assertions.assertThat(name).contains("projects/")
                    // Ensure that the token is not empty
                    Assertions.assertThat(token).isNotEmpty()
                    // Ensure that the valid is true
                    Assertions.assertThat(valid).isTrue()
                    // Ensure that the score is between 0 and 1
                    Assertions.assertThat(score).isBetween(0.0, 1.0)
                    // Ensure that the action is "login" (this is the default action)
                    Assertions.assertThat(action).isEqualTo("login")
                    // Ensure that the siteKey is the expected one
                    Assertions.assertThat(siteKey).isEqualTo(RECAPTCHA_SITE_KEY)
                    // Ensure that the userAgent is not empty
                    Assertions.assertThat(userAgent).contains("okhttp")
                    // Ensure that the userIPAddress is not empty
                    Assertions.assertThat(userIpAddress).isNotEmpty()
                    // Ensure that the androidPackageName is the expected one
                    Assertions.assertThat(androidPackageName)
                        .isEqualTo("org.forgerock.android.integration.test")

                    node.next(context, nodeListener)
                }
                super.onCallbackReceived(node)
            }
        }

        FRSession.authenticate(context, TREE, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())

        // Ensure that the journey finishes with success
        Assert.assertEquals(1, nodeCaptchaHit[0].toLong())
        Assert.assertEquals(1, nodeTextOutputHit[0].toLong())
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }

    @Test
    fun testRecaptchaEnterpriseCustomAction() = runTest {
        val nodeCaptchaHit = intArrayOf(0)
        val nodeTextOutputHit = intArrayOf(0)

        val nodeListenerFuture = object : RecaptchaEnterpriseNodeListener(
            context, "custom_action"
        ) {
            val nodeListener: NodeListener<FRSession?> = this

            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(ReCaptchaEnterpriseCallback::class.java) != null) {
                    val callback = node.getCallback(ReCaptchaEnterpriseCallback::class.java)
                    nodeCaptchaHit[0]++

                    // Execute recaptcha - this should pass...
                    runBlocking {
                        callback.execute(application = application, action = "custom_action")
                        node.next(context, nodeListener)
                    }
                }
                if (node.getCallback(TextOutputCallback::class.java) != null) {
                    // Make sure that the journey finishes with success
                    nodeTextOutputHit[0]++

                    val message = node.getCallback(TextOutputCallback::class.java).message
                    val jsonObject = JSONObject(message)
                    Logger.debug("RecaptchaEnterpriseCallbackTest", jsonObject.toString(2))

                    val action = jsonObject.getJSONObject("tokenProperties").getString("action")

                    // Ensure that the action is "custom_action"
                    Assertions.assertThat(action).isEqualTo("custom_action")

                    node.next(context, nodeListener)
                }
                super.onCallbackReceived(node)
            }
        }

        FRSession.authenticate(context, TREE, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())

        // Ensure that the journey finishes with success
        Assert.assertEquals(1, nodeCaptchaHit[0].toLong())
        Assert.assertEquals(1, nodeTextOutputHit[0].toLong())
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }

    @Test
    fun testRecaptchaEnterpriseCustomPayload() = runTest {
        val nodeCaptchaHit = intArrayOf(0)
        val nodeTextOutputHit = intArrayOf(0)

        val nodeListenerFuture = object : RecaptchaEnterpriseNodeListener(
            context, "custom_payload"
        ) {
            val nodeListener: NodeListener<FRSession?> = this

            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(ReCaptchaEnterpriseCallback::class.java) != null) {
                    val callback = node.getCallback(ReCaptchaEnterpriseCallback::class.java)
                    nodeCaptchaHit[0]++

                    val customPayloadString = """
                        {
                            "firewallPolicyEvaluation": false,
                            "express": false,
                            "transaction_data": {
                                "transaction_id": "custom-payload-1234567890",
                                "payment_method": "credit-card",
                                "card_bin": "1111",
                                "card_last_four": "1234",
                                "currency_code": "CAD",
                                "value": 12.34,
                                "user": {
                                    "email": "sdkuser@example.com"
                                },
                                "billing_address": {
                                    "recipient": "Sdk User",
                                    "address": [
                                        "3333 Random Road"
                                    ],
                                    "locality": "Courtenay",
                                    "administrative_area": "BC",
                                    "region_code": "CA",
                                    "postal_code": "V2V 2V2"
                                }
                            }
                        }
                        """.trimIndent()
                    val customPayload = JSONObject(customPayloadString)
                    // Execute recaptcha - this should pass...
                    runBlocking {
                        callback.setPayload(customPayload)
                        callback.execute(application = application)
                        node.next(context, nodeListener)
                    }
                }
                if (node.getCallback(TextOutputCallback::class.java) != null) {
                    // Make sure that the journey finishes with success
                    nodeTextOutputHit[0]++

                    val message = node.getCallback(TextOutputCallback::class.java).message
                    val jsonObject = JSONObject(message)
                    Logger.debug("RecaptchaEnterpriseCallbackTest", jsonObject.toString(2))

                    val transactionId =
                        jsonObject.getJSONObject("event").getJSONObject("transactionData")
                            .getString("transactionId")
                    val paymentMethod =
                        jsonObject.getJSONObject("event").getJSONObject("transactionData")
                            .getString("paymentMethod")
                    val cardBin = jsonObject.getJSONObject("event").getJSONObject("transactionData")
                        .getString("cardBin")
                    val cardLastFour =
                        jsonObject.getJSONObject("event").getJSONObject("transactionData")
                            .getString("cardLastFour")

                    // Ensure that the custom payload has been sent to the recaptcha assessment API
                    Assertions.assertThat(transactionId).isEqualTo("custom-payload-1234567890")
                    Assertions.assertThat(paymentMethod).isEqualTo("credit-card")
                    Assertions.assertThat(cardBin).isEqualTo("1111")
                    Assertions.assertThat(cardLastFour).isEqualTo("1234")

                    // This one is set in AM via the CaptchaEnterpriseNode.PAYLOAD shared state variable
                    val accountId = jsonObject.getJSONObject("event").getJSONObject("userInfo")
                        .getString("accountId")
                    Assertions.assertThat(accountId).isEqualTo("user_account_id_123")

                    node.next(context, nodeListener)
                }
                super.onCallbackReceived(node)
            }
        }

        FRSession.authenticate(context, TREE, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())

        // Ensure that the journey finishes with success
        Assert.assertEquals(1, nodeCaptchaHit[0].toLong())
        Assert.assertEquals(1, nodeTextOutputHit[0].toLong())
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }
}