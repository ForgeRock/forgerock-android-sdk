/*
 * Copyright (c) 2022 - 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

import org.forgerock.android.auth.Node
import org.forgerock.android.auth.NodeListener

class DummyNodeListener : NodeListener<Any> {
    override fun onSuccess(result: Any) {
    }

    override fun onException(e: Exception) {
    }

    override fun onCallbackReceived(node: Node) {
    }
}