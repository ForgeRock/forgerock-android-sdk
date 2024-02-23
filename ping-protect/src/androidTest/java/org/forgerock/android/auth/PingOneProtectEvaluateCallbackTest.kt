/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth


import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.forgerock.android.auth.callback.TextOutputCallback
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PingOneProtectEvaluateCallbackTest : BasePingOneProtectTest() {
    private val TREE = "TEST_PING_ONE_PROTECT_EVALUATE"

    @Test
    fun test01EvaluateNoInit() {
        var evaluateFailure = false
        val nodeListenerFuture: PingOneProtectNodeListener = object : PingOneProtectNodeListener(
            context, "evaluate-no-init"
        ) {
            val nodeListener: NodeListener<FRSession?> = this
            override fun onCallbackReceived(node: Node) {

                if (node.getCallback(PingOneProtectEvaluationCallback::class.java) != null) {
                    val callback = node.getCallback(
                        PingOneProtectEvaluationCallback::class.java
                    )

                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            callback.getData(context)
                        }
                        catch (e: Exception) {
                            evaluateFailure = true
                            assertThat("PingOneSignals SDK is not initialized", e.message === "PingOneSignals SDK is not initialized")
                        }
                        node.next(context, nodeListener)
                    }

                    return
                }

                if (node.getCallback(TextOutputCallback::class.java) != null) {
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
        FRSession.authenticate(context, TREE, nodeListenerFuture)
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

                if (node.getCallback(PingOneProtectInitializeCallback::class.java) != null) {
                    val callback = node.getCallback(
                        PingOneProtectInitializeCallback::class.java
                    )

                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            callback.start(context)
                        }
                        catch (e: Exception) {
                            Assert.fail("Unexpected failure during initialize!")
                        }
                        node.next(context, nodeListener)
                    }
                    return
                }

                if (node.getCallback(PingOneProtectEvaluationCallback::class.java) != null) {
                    val callback = node.getCallback(
                        PingOneProtectEvaluationCallback::class.java
                    )

                    assertThat("pauseBehavioralData is true", callback.pauseBehavioralData == true)

                    var signalsData = ""

                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            callback.getData(context)
                            // Make sure that the SDK has collected and sends the signals data
                            signalsData = callback.getInputValue(0).toString()
                        }
                        catch (e: Exception) {
                            Assert.fail("Unexpected failure during evaluate!")
                        }

                        assertThat("Signals data is not empty after collection", signalsData != "")
                        node.next(context, nodeListener)
                    }
                    return
                }

                if (node.getCallback(TextOutputCallback::class.java) != null) {
                    val callback = node.getCallback(
                        TextOutputCallback::class.java
                    )

                    assertThat("The Protect Evaluation node should trigger High, Medium or Low outcome", callback.message == "Success")
                    evaluateSuccess = true
                    node.next(context, nodeListener)
                    return
                }
                super.onCallbackReceived(node)
            }
        }
        FRSession.authenticate(context, TREE, nodeListenerFuture)
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

                if (node.getCallback(PingOneProtectInitializeCallback::class.java) != null) {
                    val callback = node.getCallback(
                        PingOneProtectInitializeCallback::class.java
                    )

                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            callback.start(context)
                        }
                        catch (e: Exception) {
                            Assert.fail("Unexpected failure during initialize!")
                        }
                        node.next(context, nodeListener)
                    }
                    return
                }

                if (node.getCallback(PingOneProtectEvaluationCallback::class.java) != null) {
                    val callback = node.getCallback(
                        PingOneProtectEvaluationCallback::class.java
                    )

                    assertThat("pauseBehavioralData is false", callback.pauseBehavioralData == false)

                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            callback.getData(context)
                        }
                        catch (e: Exception) {
                            Assert.fail("Unexpected failure during evaluate!")
                        }

                        // Make sure that the SDK has collected and sends the signals data
                        assertThat("Signals data is not empty", callback.getInputValue(0) != "")
                        node.next(context, nodeListener)
                    }
                    return
                }

                if (node.getCallback(TextOutputCallback::class.java) != null) {
                    val callback = node.getCallback(
                        TextOutputCallback::class.java
                    )

                    assertThat("The Protect Evaluation node should trigger High, Medium or Low outcome", callback.message == "Success")
                    evaluateSuccess = true
                    node.next(context, nodeListener)
                    return
                }
                super.onCallbackReceived(node)
            }
        }
        FRSession.authenticate(context, TREE, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertTrue(evaluateSuccess)

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }
}
