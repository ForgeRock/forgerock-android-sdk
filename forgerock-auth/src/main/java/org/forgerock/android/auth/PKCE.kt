/*
 * Copyright (c) 2019 - 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

/**
 * Domain object to store PKCE related data.
 */
class PKCE(val codeChallenge: String,
                    val codeChallengeMethod: String,
                    val codeVerifier: String)
