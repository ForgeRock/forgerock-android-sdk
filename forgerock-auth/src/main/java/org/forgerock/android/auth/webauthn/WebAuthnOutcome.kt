/*
 * Copyright (c) 2025 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn

import kotlinx.serialization.Serializable

@Serializable
data class WebAuthnOutcome(val authenticatorAttachment: String , val legacyData: String)
