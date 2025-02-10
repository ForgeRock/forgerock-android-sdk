/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.token

import org.forgerock.android.auth.AccessToken

data class TokenState(
    var accessToken: AccessToken? = null,
    var exception: Throwable? = null)