/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.forgerock.android.auth.callback.DeviceBindingCallback
import org.forgerock.android.auth.devicebind.UserDeviceKeyService
import org.forgerock.android.auth.devicebind.UserKey
import org.forgerock.android.auth.devicebind.UserKeyService
import org.forgerock.android.auth.exception.ApiException
import java.io.IOException

/**
 * Manage [UserKey] that created by the SDK
 * The [UserKey] are created with [DeviceBindingCallback]
 */
class FRUserKeys @JvmOverloads constructor(private val context: Context,
                                           private val userKeyService: UserKeyService = UserDeviceKeyService(
                                               context)) {

    /**
     * Load all the [UserKey] which created with [DeviceBindingCallback]
     * @return All the [UserKey]
     */
    fun loadAll(): List<UserKey> {
        return userKeyService.getAll()
    }

    /**
     * Delete user key from local storage and also remotely from Server.
     * By default, if failed to delete from server, local storage will not be deleted,
     * by providing [forceDelete] to true, it will also delete local keys if server call is failed.
     *
     * @param userKey The [UserKey] to be deleted
     * @param forceDelete Default to false, true will delete local keys even server key removal is failed.
     */
    @Throws(ApiException::class, IOException::class)
    suspend fun delete(userKey: UserKey, forceDelete: Boolean = false) {
        userKeyService.delete(userKey, forceDelete)
    }

    /**
     * Delete user key from local storage and also remotely from Server.
     * By default, if failed to delete from server, local storage will not be deleted,
     * by providing [forceDelete] to true, it will also delete local keys if server call is failed.
     *
     * @param userKey The [UserKey] to be deleted
     * @param forceDelete Default to false, true will delete local keys even server key removal is failed.
     * @param listener Listener to listen for result
     */
    fun delete(userKey: UserKey, forceDelete: Boolean = false, listener: FRListener<Void>) {
        val handler = CoroutineExceptionHandler { _, t ->
            listener.onException(t as Exception)
        }
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch(handler) {
            delete(userKey, forceDelete)
            Listener.onSuccess(listener, null)
        }
    }

}