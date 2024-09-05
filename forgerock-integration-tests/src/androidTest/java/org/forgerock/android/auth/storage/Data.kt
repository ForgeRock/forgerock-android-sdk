/*
 * Copyright (c) 2024. PingIdentity. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.storage

import kotlinx.serialization.Serializable

@Serializable
data class Data(val a: Int, val b: String)

@Serializable
data class Data2(val x: Int, val y: String)
