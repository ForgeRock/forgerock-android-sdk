/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import org.forgerock.android.auth.FRListenerFuture;
import org.forgerock.android.auth.PolicyAdvice;
import org.forgerock.android.auth.interceptor.AdviceHandler;

import java.util.concurrent.Future;

/**
 * Handler to handle Policy Advice, this handler will prompt a dialog and trigger an auth tree.
 */
public class AdviceDialogHandler implements AdviceHandler<Void> {

    @Override
    public Future<Void> onAdviceReceived(Context context, PolicyAdvice advice) {
        AdviceDialogFragment fragment = AdviceDialogFragment.newInstance(advice);
        FRListenerFuture<Void> future = new FRListenerFuture<>();
        fragment.setListener(future);
        fragment.show(((FragmentActivity) context).getSupportFragmentManager(), null);
        return future;
    }
}
