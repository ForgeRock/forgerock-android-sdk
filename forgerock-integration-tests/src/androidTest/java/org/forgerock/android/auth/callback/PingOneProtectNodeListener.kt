/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.NodeListenerFuture

open class PingOneProtectNodeListener(
    private val context: Context,
    private val nodeConfiguration: String
) : NodeListenerFuture<FRSession?>() {
    override fun onCallbackReceived(node: Node) {
        node.getCallback(ChoiceCallback::class.java)?.let {
            val choices = it.choices
            val choiceIndex = choices.indexOf(nodeConfiguration)
            it.setSelectedIndex(choiceIndex)
            node.next(context, this)
        }
        node.getCallback(NameCallback::class.java)?.let {
            it.setName(BasePingOneProtectTest.USERNAME)
            node.next(context, this)
        }
    }
}