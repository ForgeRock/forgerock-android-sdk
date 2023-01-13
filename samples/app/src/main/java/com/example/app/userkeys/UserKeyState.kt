/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.userkeys

import org.forgerock.android.auth.devicebind.UserKey

data class UserKeyState(
    var userKey: UserKey? = null)