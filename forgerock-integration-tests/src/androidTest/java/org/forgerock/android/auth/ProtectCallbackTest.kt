/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.forgerock.android.auth.callback.NameCallback
import org.junit.After
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProtectCallbackTest {

    var context: Context = ApplicationProvider.getApplicationContext()

    val TREE = "protect"

    @After
    fun logoutSession() {
        if (FRSession.getCurrentSession() != null) {
            FRSession.getCurrentSession().logout()
        }
    }


    @Test
    fun protectHappyPath(){
        val nodeListenerFuture: NodeListenerFuture<FRSession> = object : ProtectNodeListener(
            context,
        ) {
            val nodeListener: NodeListener<FRSession?> = this
            override fun onCallbackReceived(node: Node) {
                if (node.getCallback<PingOneProtectInitializeCallback>(PingOneProtectInitializeCallback::class.java) != null) {
                    val callback: PingOneProtectInitializeCallback = node.getCallback<PingOneProtectInitializeCallback>(PingOneProtectInitializeCallback::class.java)
                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            callback.start(context)
                            node.next(context, nodeListener)
                        }
                        catch (e: Exception) {
                            fail("Unexpected failure during client app integrity call!")
                            node.next(context, nodeListener)
                        }
                    }
                    return
                }

                if (node.getCallback<PingOneProtectEvaluationCallback>(PingOneProtectEvaluationCallback::class.java) != null) {
                    val callback: PingOneProtectEvaluationCallback = node.getCallback<PingOneProtectEvaluationCallback>(PingOneProtectEvaluationCallback::class.java)
                    val scope = CoroutineScope(Dispatchers.Default)
                    scope.launch {
                        try {
                            callback.getData(context)
                            node.next(context, nodeListener)
                        }
                        catch (e: Exception) {
                            fail("Unexpected failure during client app integrity call!")
                            node.next(context, nodeListener)
                        }

                    }
                    return
                }

                if (node.getCallback<NameCallback?>(NameCallback::class.java) != null) {
                    node.getCallback<NameCallback>(NameCallback::class.java)
                        .setName("andy")
                    node.next(context, nodeListener)
                    return
                }

            }
        }
        FRSession.authenticate(context, TREE, nodeListenerFuture)
        Assert.assertNotNull(nodeListenerFuture.get())

        // Ensure that the journey finishes with success
        Assert.assertNotNull(FRSession.getCurrentSession())
        Assert.assertNotNull(FRSession.getCurrentSession().sessionToken)

    }

    @Before
    fun setUpSDK() {
        Logger.set(Logger.Level.DEBUG)

        // Prepare dynamic configuration object

        val options = FROptionsBuilder.build {
            server {
                url = "https://openam-protect2.forgeblocks.com/am"
                realm = "alpha"
                cookieName = "c1c805de4c9b333"
                timeout = 50
            }
            oauth {
                oauthClientId = "AndroidTest"
                oauthRedirectUri = "org.forgerock.demo:/oauth2redirect"
                oauthCacheSeconds = 0
                oauthScope = "openid profile email address phone"
                oauthThresholdSeconds = 0
            }
            service {
                authServiceName = "protect"
            }
        }

        FRAuth.start(context, options)
    }

}


open class ProtectNodeListener(
    private val context: Context) :
    NodeListenerFuture<FRSession>() {
    override fun onCallbackReceived(node: Node) {}
}

