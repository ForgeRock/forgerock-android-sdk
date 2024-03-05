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
import org.forgerock.android.auth.PingOneProtectInitializeCallback
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PingOneProtectInitializeCallbackTest : BasePingOneProtectTest() {
    private val authenticationTree = "TEST_PING_ONE_PROTECT_INITIALIZE"

    @Test
    fun testProtectInitializeDefaults() {
        val nodeListenerFuture: PingOneProtectNodeListener = object : PingOneProtectNodeListener(
            context, "init-default"
        ) {
            val nodeListener: NodeListener<FRSession?> = this
            override fun onCallbackReceived(node: Node) {
                node.getCallback(PingOneProtectInitializeCallback::class.java)?.let {
                    assertThat("envId should not be empty string", it.envId != "")
                    assertThat("consoleLogEnabled should be false", it.consoleLogEnabled == false)
                    assertThat("deviceAttributesToIgnore should be an empty list", it.deviceAttributesToIgnore?.isEmpty() ?: false)
                    assertThat("customHost should be empty string", it.customHost == "")
                    assertThat("lazyMetadata should be false", it.lazyMetadata == false)
                    assertThat("behavioralDataCollection should be true", it.behavioralDataCollection == true)

                    node.next(context, nodeListener)
                    return
                }
                super.onCallbackReceived(node)
            }
        }
        FRSession.authenticate(context, authenticationTree, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }

    @Test
    fun testProtectInitializeCustom() {
        val nodeListenerFuture: PingOneProtectNodeListener = object : PingOneProtectNodeListener(
            context, "init-custom"
        ) {
            val nodeListener: NodeListener<FRSession?> = this
            override fun onCallbackReceived(node: Node) {
                node.getCallback(PingOneProtectInitializeCallback::class.java)?.let {
                    assertThat("envId should not be empty string", it.envId != "")
                    assertThat("consoleLogEnabled should be true", it.consoleLogEnabled == true)
                    assertThat("deviceAttributesToIgnore contains 'Model'",
                        it.deviceAttributesToIgnore!!.contains("Model") );
                    assertThat("deviceAttributesToIgnore contains 'Manufacturer'",
                        it.deviceAttributesToIgnore!!.contains("Manufacturer") );
                    assertThat("deviceAttributesToIgnore contains 'Screen size'",
                        it.deviceAttributesToIgnore!!.contains("Screen size") );
                    assertThat("customHost should be empty string", it.customHost == "custom.host.com")
                    assertThat("lazyMetadata should be true", it.lazyMetadata == true)
                    assertThat("behavioralDataCollection should be false", it.behavioralDataCollection == false)

                    node.next(context, nodeListener)
                    return
                }
                super.onCallbackReceived(node)
            }
        }
        FRSession.authenticate(context, authenticationTree, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }

    @Test
    fun testProtectInitializeClientError() {
        var failureTriggered = false

        val nodeListenerFuture: PingOneProtectNodeListener = object : PingOneProtectNodeListener(
            context, "init-error"
        ) {
            val nodeListener: NodeListener<FRSession?> = this
            override fun onCallbackReceived(node: Node) {
                node.getCallback(PingOneProtectInitializeCallback::class.java)?.let {
                    it.setClientError("Failed to initialize")

                    node.next(context, nodeListener)
                    return
                }
                node.getCallback(TextOutputCallback::class.java)?.let {
                    assertThat("The Protect Initialize node should trigger the 'false' outcome", it.message == "Failure")
                    failureTriggered = true
                    node.next(context, nodeListener)
                    return
                }
                super.onCallbackReceived(node)
            }
        }
        FRSession.authenticate(context, authenticationTree, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())
        Assert.assertTrue(failureTriggered)

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }

    @Test
    fun testStartProtect() {
        val nodeListenerFuture: PingOneProtectNodeListener = object : PingOneProtectNodeListener(
            context, "init-default"
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

                node.getCallback(TextOutputCallback::class.java)?.let {
                    assertThat("The Protect Initialize node should trigger the 'false' outcome", it.message == "Failure")
                    node.next(context, nodeListener)
                    return
                }

                super.onCallbackReceived(node)
            }
        }
        FRSession.authenticate(context, authenticationTree, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }
}