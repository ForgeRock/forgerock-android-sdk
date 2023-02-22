/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.userprofile

import org.forgerock.android.auth.UserInfo

data class UserProfileState(
    var user: UserInfo? = null,
    var exception: Exception? = null)