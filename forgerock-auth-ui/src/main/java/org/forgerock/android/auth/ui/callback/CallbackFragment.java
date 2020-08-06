/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.callback.Callback;

import static org.forgerock.android.auth.ui.CallbackFragmentFactory.CALLBACK;
import static org.forgerock.android.auth.ui.CallbackFragmentFactory.NODE;

/**
 * UI Fragment which represent a Callback
 *
 * @param <T> The Callback Class defined or registered under {@link org.forgerock.android.auth.ui.CallbackFragmentFactory}
 */
public abstract class CallbackFragment<T extends Callback> extends Fragment {


    private CallbackController callbackController;

    /**
     * The Callback's Node {@link Node}
     */
    protected Node node;

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
            node = (Node) getArguments().getSerializable(NODE);
            callback = (T) getArguments().getSerializable(CALLBACK);
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

    /**
     * Call when data is collected from the callback.
     */
    public void onDataCollected() {
        callbackController.onDataCollected(callback);
    }

    /**
     * Proceed to next node from the intelligent tree.
     */
    public void next() {
        onDataCollected();
        callbackController.next();
    }

    /**
     * Cancel the authentication and exist the intelligent tree
     *
     * @param e The exception cause to exist the intelligent tree.
     */
    public void cancel(Exception e) {
        callbackController.cancel(e);
    }

    /**
     * Suspend the current authentication flow.
     */
    public void suspend() {
        callbackController.suspend();
    }
}
