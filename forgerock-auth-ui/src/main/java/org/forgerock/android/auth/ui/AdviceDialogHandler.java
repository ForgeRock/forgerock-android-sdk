/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.forgerock.android.auth.PolicyAdvice;
import org.forgerock.android.auth.interceptor.AdviceHandler;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;

/**
 * Handler to handle Policy Advice, this handler will prompt a dialog and trigger an auth tree.
 */
public class AdviceDialogHandler implements AdviceHandler {

    @Nullable
    @Override
    public Object onAdviceReceived(@NonNull Context context, @NonNull PolicyAdvice advice, @NonNull Continuation<? super Unit> $completion) {
        AdviceDialogFragment fragment = AdviceDialogFragment.newInstance(advice);
        fragment.setListener($completion);
        fragment.show(((FragmentActivity) context).getSupportFragmentManager(), null);
        return IntrinsicsKt.getCOROUTINE_SUSPENDED();
    }
}
