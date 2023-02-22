/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.centralize

import org.forgerock.android.auth.FRUser

data class CentralizeState(
    var user: FRUser? = null,
    var exception: Exception? = null)