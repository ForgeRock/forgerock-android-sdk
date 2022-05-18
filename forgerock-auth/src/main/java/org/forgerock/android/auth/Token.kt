/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import java.io.Serializable

/**
 * Domain object to hold generic Token
 */
open class Token(val value: String) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Token) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}