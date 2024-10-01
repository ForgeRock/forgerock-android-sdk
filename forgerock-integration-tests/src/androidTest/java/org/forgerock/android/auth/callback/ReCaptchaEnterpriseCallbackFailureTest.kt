/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import com.google.android.recaptcha.RecaptchaException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.NodeListener
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Failure e2e tests for the [ReCaptchaEnterpriseCallback]
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // These tests must run in order
class ReCaptchaEnterpriseCallbackFailureTest : ReCaptchaEnterpriseCallbackBaseTest() {

    @Test
    fun test01RecaptchaEnterpriseScoreFailure() = runTest {
        val nodeCaptchaHit = intArrayOf(0)
        val nodeTextOutputHit = intArrayOf(0)

        val nodeListenerFuture = object : RecaptchaEnterpriseNodeListener(
            context, "score_failure"
        ) {
            val nodeListener: NodeListener<FRSession?> = this

            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(ReCaptchaEnterpriseCallback::class.java) != null) {
                    val callback = node.getCallback(ReCaptchaEnterpriseCallback::class.java)
                    nodeCaptchaHit[0]++

                    // Execute recaptcha - this should pass...
                    runBlocking {
                        callback.execute(application = application)
                        node.next(context, nodeListener)
                    }
                }
                if (node.getCallback(TextOutputCallback::class.java) != null) {
                    nodeTextOutputHit[0]++

                    // Note: The journey sends back to us the content of CaptchaEnterpriseNode.FAILURE
                    // in the message of the TextOutputCallback... so we can parse it here and verify results...
                    val message = node.getCallback(TextOutputCallback::class.java).message
                    Assertions.assertThat(message).contains("VALIDATION_ERROR")

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
    fun test02RecaptchaEnterpriseInvalidProjectId() = runTest {
        val nodeCaptchaHit = intArrayOf(0)
        val nodeTextOutputHit = intArrayOf(0)

        val nodeListenerFuture = object : RecaptchaEnterpriseNodeListener(
            context, "invalid_project_id"
        ) {
            val nodeListener: NodeListener<FRSession?> = this

            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(ReCaptchaEnterpriseCallback::class.java) != null) {
                    val callback = node.getCallback(ReCaptchaEnterpriseCallback::class.java)
                    nodeCaptchaHit[0]++
                    runBlocking {
                        callback.execute(application = application)
                        node.next(context, nodeListener)
                    }
                }
                if (node.getCallback(TextOutputCallback::class.java) != null) {
                    nodeTextOutputHit[0]++

                    // Note: The journey sends back to us the content of CaptchaEnterpriseNode.FAILURE
                    // in the message of the TextOutputCallback... so we can parse it here and verify results...
                    val message = node.getCallback(TextOutputCallback::class.java).message
                    Assertions.assertThat(message).contains("API_ERROR")
                    Assertions.assertThat(message)
                        .contains("reCAPTCHA Enterprise API has not been used in project invalid before or it is disabled")

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
    fun test03RecaptchaEnterpriseInvalidVerificationUrl() = runTest {
        val nodeCaptchaHit = intArrayOf(0)
        val nodeTextOutputHit = intArrayOf(0)

        val nodeListenerFuture = object : RecaptchaEnterpriseNodeListener(
            context, "invalid_verification_url"
        ) {
            val nodeListener: NodeListener<FRSession?> = this

            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(ReCaptchaEnterpriseCallback::class.java) != null) {
                    val callback = node.getCallback(ReCaptchaEnterpriseCallback::class.java)
                    nodeCaptchaHit[0]++
                    runBlocking {
                        callback.execute(application = application)
                        node.next(context, nodeListener)
                    }
                }
                if (node.getCallback(TextOutputCallback::class.java) != null) {
                    nodeTextOutputHit[0]++

                    // Note: The journey sends back to us the content of CaptchaEnterpriseNode.FAILURE
                    // in the message of the TextOutputCallback... so we can parse it here and verify results...
                    val message = node.getCallback(TextOutputCallback::class.java).message
                    Assertions.assertThat(message).contains("API_ERROR")

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
    fun test04RecaptchaEnterpriseInvalidSecretKey() = runTest {
        val nodeCaptchaHit = intArrayOf(0)
        val nodeTextOutputHit = intArrayOf(0)

        val nodeListenerFuture = object : RecaptchaEnterpriseNodeListener(
            context, "invalid_secret_key"
        ) {
            val nodeListener: NodeListener<FRSession?> = this

            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(ReCaptchaEnterpriseCallback::class.java) != null) {
                    val callback = node.getCallback(ReCaptchaEnterpriseCallback::class.java)
                    nodeCaptchaHit[0]++
                    runBlocking {
                        callback.execute(application = application)
                        node.next(context, nodeListener)
                    }
                }
                if (node.getCallback(TextOutputCallback::class.java) != null) {
                    nodeTextOutputHit[0]++

                    // Note: The journey sends back to us the content of CaptchaEnterpriseNode.FAILURE
                    // in the message of the TextOutputCallback... so we can parse it here and verify results...
                    val message = node.getCallback(TextOutputCallback::class.java).message
                    Assertions.assertThat(message)
                        .contains("INVALID_SECRET_KEY:Secret key could not be retrieved")

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
    fun test05RecaptchaEnterpriseCustomClientError() = runTest {
        val nodeCaptchaHit = intArrayOf(0)
        val nodeTextOutputHit = intArrayOf(0)

        val nodeListenerFuture = object : RecaptchaEnterpriseNodeListener(
            context, "custom_client_error"
        ) {
            val nodeListener: NodeListener<FRSession?> = this

            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(ReCaptchaEnterpriseCallback::class.java) != null) {
                    val callback = node.getCallback(ReCaptchaEnterpriseCallback::class.java)
                    nodeCaptchaHit[0]++
                    runBlocking {
                        callback.setClientError("CUSTOM_CLIENT_ERROR")
                        callback.execute(application = application)
                        node.next(context, nodeListener)
                    }
                }
                if (node.getCallback(TextOutputCallback::class.java) != null) {
                    nodeTextOutputHit[0]++

                    // Note: The journey sends back to us the content of CaptchaEnterpriseNode.FAILURE
                    // in the message of the TextOutputCallback... so we can parse it here and verify results...
                    val message = node.getCallback(TextOutputCallback::class.java).message
                    Assertions.assertThat(message).contains("CUSTOM_CLIENT_ERROR")

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
    fun test06RecaptchaEnterpriseInvalidSiteKey() = runTest {
        val nodeCaptchaHit = intArrayOf(0)
        val nodeTextOutputHit = intArrayOf(0)
        val recaptchaExceptionHit = intArrayOf(0)

        val nodeListenerFuture = object : RecaptchaEnterpriseNodeListener(
            context, "invalid_site_key"
        ) {
            val nodeListener: NodeListener<FRSession?> = this

            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(ReCaptchaEnterpriseCallback::class.java) != null) {
                    val callback = node.getCallback(ReCaptchaEnterpriseCallback::class.java)
                    nodeCaptchaHit[0]++

                    try {
                        runBlocking {
                            callback.execute(application = application)
                        }
                    } catch (e: Exception) {
                        recaptchaExceptionHit[0]++
                        if (e is RecaptchaException) {
                            Logger.error("RecaptchaException", "${e.errorCode}:${e.message}")
                        }
                        Logger.error("RecaptchaException", e.message)
                    }
                    node.next(context, nodeListener)
                }
                if (node.getCallback(TextOutputCallback::class.java) != null) {
                    nodeTextOutputHit[0]++

                    // Note: The journey sends back to us the content of CaptchaEnterpriseNode.FAILURE
                    // in the message of the TextOutputCallback... so we can parse it here and verify results...
                    val message = node.getCallback(TextOutputCallback::class.java).message
                    Assertions.assertThat(message).contains("CLIENT_ERROR")

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
        Assert.assertEquals(1, recaptchaExceptionHit[0].toLong())
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }
}