/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.NodeListener
import org.forgerock.android.auth.PingOneProtectEvaluationCallback
import org.forgerock.android.auth.PingOneProtectInitializeCallback
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PingOneProtectEvaluateCallbackTest : BasePingOneProtectTest() {
    private val authenticationTree = "TEST_PING_ONE_PROTECT_EVALUATE"

    @Test
    fun test01EvaluateNoInit() {
        var evaluateFailure = false
        val nodeListenerFuture: PingOneProtectNodeListener = object : PingOneProtectNodeListener(
            context, "evaluate-no-init"
        ) {
            val nodeListener: NodeListener<FRSession?> = this
            override fun onCallbackReceived(node: Node) {
                node.getCallback(PingOneProtectEvaluationCallback::class.java)?.let {
                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            it.getData(context)
                        }
                        catch (e: Exception) {
                            evaluateFailure = true
                            assertThat("PingOneSignals SDK is not initialized", e.message === "PingOneSignals SDK is not initialized")
                        }
                        node.next(context, nodeListener)
                    }
                    return
                }

                node.getCallback(TextOutputCallback::class.java)?.let {
                    val callback = node.getCallback(
                        TextOutputCallback::class.java
                    )

                    assertThat("The Protect Evaluation node should trigger the Client Error outcome", callback.message == "Client Error")
                    node.next(context, nodeListener)
                    return
                }
                super.onCallbackReceived(node)
            }
        }

        FRSession.authenticate(context, authenticationTree, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertTrue(evaluateFailure)
        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }

    @Test
    fun test02EvaluateSuccess() {
        var evaluateSuccess = false
        val nodeListenerFuture: PingOneProtectNodeListener = object : PingOneProtectNodeListener(
            context, "evaluate-default"
        ) {
            val nodeListener: NodeListener<FRSession?> = this
            override fun onCallbackReceived(node: Node) {
                node.getCallback(PingOneProtectInitializeCallback::class.java)?.let {
                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            it.start(context)
                        }
                        catch (e: Exception) {
                            Assert.fail("Unexpected failure during initialize!")
                        }
                        node.next(context, nodeListener)
                    }
                    return
                }

                node.getCallback(PingOneProtectEvaluationCallback::class.java)?.let {
                    assertThat("pauseBehavioralData is true", it.pauseBehavioralData == true)

                    var signalsData = ""
                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            it.getData(context)
                            // Make sure that the SDK has collected and sends the signals data
                            signalsData = it.getInputValue(0).toString()
                        }
                        catch (e: Exception) {
                            Assert.fail("Unexpected failure during evaluate!")
                        }

                        assertThat("Signals data is not empty after collection", signalsData != "")
                        node.next(context, nodeListener)
                    }
                    return
                }

                node.getCallback(TextOutputCallback::class.java)?.let {
                    assertThat("The Protect Evaluation node should trigger High, Medium or Low outcome", it.message == "Success")
                    evaluateSuccess = true
                    node.next(context, nodeListener)
                    return
                }
                super.onCallbackReceived(node)
            }
        }
        FRSession.authenticate(context, authenticationTree, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertTrue(evaluateSuccess)

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }

    @Test
    fun test03EvaluatePauseBehavioralDataOff() {
        var evaluateSuccess = false
        val nodeListenerFuture: PingOneProtectNodeListener = object : PingOneProtectNodeListener(
            context, "evaluate-pause-off"
        ) {
            val nodeListener: NodeListener<FRSession?> = this
            override fun onCallbackReceived(node: Node) {
                node.getCallback(PingOneProtectInitializeCallback::class.java)?.let {
                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            it.start(context)
                        }
                        catch (e: Exception) {
                            Assert.fail("Unexpected failure during initialize!")
                        }
                        node.next(context, nodeListener)
                    }
                    return
                }

                node.getCallback(PingOneProtectEvaluationCallback::class.java)?.let {
                    assertThat("pauseBehavioralData is false", it.pauseBehavioralData == false)

                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            it.getData(context)
                        }
                        catch (e: Exception) {
                            Assert.fail("Unexpected failure during evaluate!")
                        }

                        // Make sure that the SDK has collected and sends the signals data
                        assertThat("Signals data is not empty", it.getInputValue(0) != "")
                        node.next(context, nodeListener)
                    }
                    return
                }

                node.getCallback(TextOutputCallback::class.java)?.let {
                    assertThat("The Protect Evaluation node should trigger High, Medium or Low outcome", it.message == "Success")
                    evaluateSuccess = true
                    node.next(context, nodeListener)
                    return
                }
                super.onCallbackReceived(node)
            }
        }

        FRSession.authenticate(context, authenticationTree, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertTrue(evaluateSuccess)

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }
}