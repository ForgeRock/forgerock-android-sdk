/*
 * Copyright (c) 2022 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import org.forgerock.android.auth.callback.ChoiceCallback
import org.forgerock.android.auth.callback.NameCallback

open class PingOneProtectNodeListener(
    private val context: Context,
    private val nodeConfiguration: String
) : NodeListenerFuture<FRSession?>() {
    override fun onCallbackReceived(node: Node) {
        if (node.getCallback(ChoiceCallback::class.java) != null) {
            val choiceCallback = node.getCallback(
                ChoiceCallback::class.java
            )
            val choices = choiceCallback.choices
            val choiceIndex = choices.indexOf(nodeConfiguration)
            choiceCallback.setSelectedIndex(choiceIndex)
            node.next(context, this)
        }
        if (node.getCallback(NameCallback::class.java) != null) {
            node.getCallback(NameCallback::class.java).setName(BasePingOneProtectTest.USERNAME)
            node.next(context, this)
        }
    }
}
