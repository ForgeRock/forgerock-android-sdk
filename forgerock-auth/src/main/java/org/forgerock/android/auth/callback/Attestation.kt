/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

sealed class Attestation(val challenge: ByteArray? = null) : java.io.Serializable {
    object None : Attestation()
    class Default(challenge: ByteArray) : Attestation(challenge)
    class Custom(challenge: ByteArray) : Attestation(challenge)

    companion object {
        fun fromString(value: String, challenge: ByteArray): Attestation =
            when (value.lowercase()) {
                "none" -> None
                "default" -> Default(challenge)
                "custom" -> Custom(challenge)
                else -> throw java.lang.IllegalArgumentException("Unsupported attestation parameter")
            }
    }
}


