/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.interceptor

import android.content.Context
import androidx.annotation.WorkerThread
import org.forgerock.android.auth.PolicyAdvice
import java.util.concurrent.Future

/**
 * Handler to handle the Advice
 */
interface AdviceHandler {
    /**
     * Called when an Advice is received
     *
     * @param context The current Activity context
     * @param advice The received Advice
     */
    @WorkerThread
    suspend fun onAdviceReceived(context: Context, advice: PolicyAdvice)
}