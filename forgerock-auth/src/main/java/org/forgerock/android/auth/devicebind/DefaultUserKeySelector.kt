/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class DefaultUserKeySelector : UserKeySelector {

    override suspend fun selectUserKey(userKeys: UserKeys,
                                       fragmentActivity: FragmentActivity): UserKey =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val existing =
                    fragmentActivity.supportFragmentManager.findFragmentByTag(DeviceBindFragment.TAG) as? DeviceBindFragment
                if (existing != null) {
                    existing.continuation = continuation
                } else {
                    DeviceBindFragment.newInstance(userKeys, continuation)
                        .apply {
                            this.show(fragmentActivity.supportFragmentManager,
                                DeviceBindFragment.TAG)
                        }
                }
            }
        }
}