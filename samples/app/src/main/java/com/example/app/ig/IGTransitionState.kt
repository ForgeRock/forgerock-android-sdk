/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package com.example.app.ig

import com.google.android.gms.tasks.Continuation
import kotlinx.coroutines.CancellableContinuation
import okhttp3.Response
import org.forgerock.android.auth.FRListenerFuture
import org.forgerock.android.auth.PolicyAdvice
import java.lang.Exception

sealed class IGTransitionState {
    object Start : IGTransitionState()
    data class Authenticate(val policyAdvice: PolicyAdvice, val continuation: CancellableContinuation<Unit>) :
        IGTransitionState()

    data class Finished(val result: String? = null, val exception: Exception? = null) :
        IGTransitionState()
}