/*
 * Copyright (c) 2022 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import kotlinx.serialization.Serializable

/**
 * Domain object to hold generic Token
 */
@Serializable
sealed interface Token {
    val value: String
}