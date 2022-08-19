/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import org.assertj.core.api.Assertions
import org.forgerock.android.auth.*
import org.junit.Assert.assertEquals
import org.junit.Ignore
import java.lang.Exception
import java.util.concurrent.ExecutionException

@Ignore
class DeviceBindingCallbackTest : TreeTest() {
    private var hit = 0
    override fun getTreeName(): String {
        return "devicebind"
    }

    override fun getNodeListenerFuture(): NodeListenerFuture<FRSession> {
        return object : UsernamePasswordNodeListener(context) {
            override fun onCallbackReceived(node: Node) {
                val listener = this
                if (node.getCallback(DeviceBindingCallback::class.java) != null) {
                    val callback = node.getCallback(
                        DeviceBindingCallback::class.java
                    )
                    callback.bind(context, object: FRListener<Void> {
                        override fun onSuccess(result: Void?) {
                            node.next(context, listener)
                        }

                        override fun onException(e: Exception?) {
                            node.next(context, listener)
                        }

                    })
                    hit++
                    return
                }
                super.onCallbackReceived(node)
            }
        }
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    override fun testTree() {
        super.testTree()
        Assertions.assertThat(hit).isEqualTo(1)
    }
}