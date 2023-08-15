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

    companion object {
        fun fromBoolean(value: Boolean, challenge: ByteArray): Attestation =
            if (value) {
                Default(challenge)
            } else {
                None
            }
    }
}


