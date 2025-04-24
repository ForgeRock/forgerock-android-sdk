/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.Node
import org.forgerock.android.auth.NodeListenerFuture

open class RecaptchaEnterpriseNodeListener(
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
            node.getCallback(NameCallback::class.java)
                .setName(ReCaptchaEnterpriseCallbackBaseTest.USERNAME)
            node.next(context, this)
        }
    }
}