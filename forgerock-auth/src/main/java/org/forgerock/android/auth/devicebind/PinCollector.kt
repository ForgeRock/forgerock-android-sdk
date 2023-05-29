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
 * Pin Collector interface to collect application pin
 */
interface PinCollector {

    /**
     * Collect the Application Pin.
     * @param prompt Prompt information from the Node
     * @param fragmentActivity The Current [FragmentActivity]
     */
    suspend fun collectPin(prompt: Prompt,
                           fragmentActivity: FragmentActivity = InitProvider.getCurrentActivityAsFragmentActivity()): CharArray
}