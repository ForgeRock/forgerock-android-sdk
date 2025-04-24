/*
 * Copyright (c) 2024 - 2025 Ping Identity Corporation. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import kotlinx.serialization.Serializable

@Serializable
data class SSOToken(override val value: String,
                    val successUrl: String = "",
                    val realm: String = "") : Token