/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app

import androidx.lifecycle.ViewModel
import org.forgerock.android.auth.FRUser

class LogoutViewModel : ViewModel() {

    fun logout() {
        FRUser.getCurrentUser()?.logout()
    }

}