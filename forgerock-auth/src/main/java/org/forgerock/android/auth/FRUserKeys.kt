/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import android.content.Context
import org.forgerock.android.auth.callback.DeviceBindingCallback
import org.forgerock.android.auth.devicebind.UserDeviceKeyService
import org.forgerock.android.auth.devicebind.UserKey
import org.forgerock.android.auth.devicebind.UserKeyService

/**
 * Manage [UserKey] that created by the SDK
 * The [UserKey] are created with [DeviceBindingCallback]
 */
class FRUserKeys @JvmOverloads constructor(private val context: Context,
                                           private val userKeyService: UserKeyService = UserDeviceKeyService(context)) {

    /**
     * Load all the [UserKey] which created with [DeviceBindingCallback]
     * @return All the [UserKey]
     */
    fun loadAll(): List<UserKey> {
        return userKeyService.userKeys
    }

    /**
     * Delete the [UserKey]
     * @param userKey The [UserKey] to be deleted
     */
    fun delete(userKey: UserKey) {
        userKeyService.delete(userKey)
    }

}