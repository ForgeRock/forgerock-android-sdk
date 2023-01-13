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

class DefaultPinCollector : PinCollector {

    override suspend fun collectPin(prompt: Prompt, fragmentActivity: FragmentActivity): CharArray =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val existing =
                    fragmentActivity.supportFragmentManager.findFragmentByTag(ApplicationPinFragment.TAG) as? ApplicationPinFragment
                existing?.let {
                    existing.continuation = continuation
                } ?: run {
                    ApplicationPinFragment.newInstance(prompt, continuation)
                        .show(fragmentActivity.supportFragmentManager, ApplicationPinFragment.TAG)
                }
            }
        }

}