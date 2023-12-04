/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

import org.forgerock.android.auth.Node

/**
 * For [Callback] which need to have awareness of the parent [Node]
 */
interface NodeAware {

    /**
     * Inject the [Node] object
     */
    fun setNode(node: Node)
}