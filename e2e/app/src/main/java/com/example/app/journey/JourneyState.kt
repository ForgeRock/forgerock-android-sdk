/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.journey

import org.forgerock.android.auth.FRSession
import org.forgerock.android.auth.Node

data class JourneyState(val node: Node? = null,
                        var exception: Exception? = null,
                        var session: FRSession? = null) {
}