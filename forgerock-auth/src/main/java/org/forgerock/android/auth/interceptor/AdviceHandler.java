/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.interceptor;

import android.content.Context;

import androidx.annotation.WorkerThread;

import org.forgerock.android.auth.PolicyAdvice;

import java.util.concurrent.Future;

/**
 * Handler to handle the Advice
 */
public interface AdviceHandler<T> {

    /**
     * Called when an Advice is received
     *
     * @param context The current Activity context
     * @param advice The received Advice
     * @return A Future representing pending completion of handling the advice
     */
    @WorkerThread
    Future<T> onAdviceReceived(Context context, PolicyAdvice advice);


}
