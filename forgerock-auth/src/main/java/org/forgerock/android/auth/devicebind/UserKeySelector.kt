/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import androidx.fragment.app.FragmentActivity
import org.forgerock.android.auth.InitProvider

/**
 * [UserKey] selector interface to select [UserKey]
 */
interface UserKeySelector {

    /**
     * Select the [UserKey] for authentication
     * @param userKeys List of [UserKey]
     * @param fragmentActivity The Current [FragmentActivity]
     * @return The selected [UserKey] for authentication
     */
    suspend fun selectUserKey(userKeys: UserKeys,
                              fragmentActivity: FragmentActivity = InitProvider.getCurrentActivityAsFragmentActivity()): UserKey
}