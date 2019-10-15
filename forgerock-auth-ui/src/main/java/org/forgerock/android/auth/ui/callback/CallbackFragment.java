/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.ui.CallbackFragmentFactory;

/**
 * UI Fragment which represent a Callback
 *
 * @param <T> The Callback Class defined or registered under {@link org.forgerock.android.auth.ui.CallbackFragmentFactory}
 */
public abstract class CallbackFragment<T extends Callback> extends Fragment {


    private CallbackController callbackController;

    /**
     * The Callback class see {@link Callback}
     */
    protected T callback;

    public CallbackFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            callback = (T) getArguments().getSerializable(CallbackFragmentFactory.CALLBACK);
        }

        if (getParentFragment() instanceof CallbackController) {
            this.callbackController = (CallbackController) getParentFragment();
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CallbackController) {
            callbackController = (CallbackController) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbackController = null;
    }

    public void onDataCollected() {
        callbackController.onDataCollected(callback);
    }

    public void next() {
        onDataCollected();
        callbackController.next();
    }

}
