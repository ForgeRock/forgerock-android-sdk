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
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PingOneProtectInitializeCallbackTest : BasePingOneProtectTest() {
    private val TREE = "TEST_PING_ONE_PROTECT_INITIALIZE"

    @Test
    fun testProtectInitializeDefaults() {
        val nodeListenerFuture: PingOneProtectNodeListener = object : PingOneProtectNodeListener(
            context, "init-default"
        ) {
            val nodeListener: NodeListener<FRSession?> = this
            override fun onCallbackReceived(node: Node) {
                if (node.getCallback(PingOneProtectInitializeCallback::class.java) != null) {
                    val callback = node.getCallback(
                        PingOneProtectInitializeCallback::class.java
                    )

                    assertThat("envId should not be empty string", callback.envId != "")
                    assertThat("consoleLogEnabled should be false", callback.consoleLogEnabled == false)
                    assertThat("deviceAttributesToIgnore should be an empty list", callback.deviceAttributesToIgnore?.isEmpty() ?: false)
                    assertThat("customHost should be empty string", callback.customHost == "")
                    assertThat("lazyMetadata should be false", callback.lazyMetadata == false)
                    assertThat("behavioralDataCollection should be true", callback.behavioralDataCollection == true)

                    node.next(context, nodeListener)
                    return
                }
                super.onCallbackReceived(node)
            }
        }
        FRSession.authenticate(context, TREE, nodeListenerFuture)
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
                if (node.getCallback(PingOneProtectInitializeCallback::class.java) != null) {
                    val callback = node.getCallback(
                        PingOneProtectInitializeCallback::class.java
                    )

                    assertThat("envId should not be empty string", callback.envId != "")
                    assertThat("consoleLogEnabled should be true", callback.consoleLogEnabled == true)
                    assertThat("deviceAttributesToIgnore contains 'Model'",
                        callback.deviceAttributesToIgnore!!.contains("Model") );
                    assertThat("deviceAttributesToIgnore contains 'Manufacturer'",
                        callback.deviceAttributesToIgnore!!.contains("Manufacturer") );
                    assertThat("deviceAttributesToIgnore contains 'Screen size'",
                        callback.deviceAttributesToIgnore!!.contains("Screen size") );
                    assertThat("customHost should be empty string", callback.customHost == "custom.host.com")
                    assertThat("lazyMetadata should be true", callback.lazyMetadata == true)
                    assertThat("behavioralDataCollection should be false", callback.behavioralDataCollection == false)

                    node.next(context, nodeListener)
                    return
                }
                super.onCallbackReceived(node)
            }
        }
        FRSession.authenticate(context, TREE, nodeListenerFuture)
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
                if (node.getCallback(PingOneProtectInitializeCallback::class.java) != null) {
                    val callback = node.getCallback(
                        PingOneProtectInitializeCallback::class.java
                    )

                    callback.setClientError("Failed to initialize")

                    node.next(context, nodeListener)
                    return
                }
                if (node.getCallback<TextOutputCallback?>(TextOutputCallback::class.java) != null) {
                    val callback = node.getCallback(
                        TextOutputCallback::class.java
                    )

                    assertThat("The Protect Initialize node should trigger the 'false' outcome", callback.message == "Failure")
                    failureTriggered = true
                    node.next(context, nodeListener)
                    return
                }
                super.onCallbackReceived(node)
            }
        }
        FRSession.authenticate(context, TREE, nodeListenerFuture)
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
                if (node.getCallback<PingOneProtectInitializeCallback?>(PingOneProtectInitializeCallback::class.java) != null) {
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

                if (node.getCallback<TextOutputCallback?>(TextOutputCallback::class.java) != null) {
                    val callback = node.getCallback(
                        TextOutputCallback::class.java
                    )

                    assertThat("The Protect Initialize node should trigger the 'false' outcome", callback.message == "Failure")
                    node.next(context, nodeListener)
                    return
                }

                super.onCallbackReceived(node)
            }
        }
        FRSession.authenticate(context, TREE, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)
    }
}
