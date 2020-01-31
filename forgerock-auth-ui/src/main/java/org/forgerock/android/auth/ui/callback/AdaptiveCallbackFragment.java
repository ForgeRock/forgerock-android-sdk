/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import android.content.Context;
import android.os.Bundle;
import android.os.OperationCanceledException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.ui.AuthHandler;
import org.forgerock.android.auth.ui.AuthenticationExceptionListener;
import org.forgerock.android.auth.ui.CallbackFragmentFactory;
import org.forgerock.android.auth.ui.R;


/**
 * This Callback Fragment having the ability to change to suit different callback conditions.
 * The parent Fragment for all {@link CallbackFragment}
 */
public class AdaptiveCallbackFragment extends Fragment implements AuthenticationExceptionListener, CallbackController {

    private Node current;
    private LinearLayout errorLayout;
    private AuthHandler authHandler;

    public AdaptiveCallbackFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            current = (Node) getArguments().getSerializable(CallbackFragmentFactory.NODE);
        }
        setAuthHandler(getParentFragment());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        setAuthHandler(context);
    }

    private void setAuthHandler(Object o) {
        if (o instanceof AuthHandler) {
            this.authHandler = (AuthHandler) o;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_callbacks, container, false);
        errorLayout = view.findViewById(R.id.error);
        Button nextButton = view.findViewById(R.id.next);
        Button cancelButton = view.findViewById(R.id.cancel);

        //Add callback to LinearLayout Vertically
        if (savedInstanceState == null) {
            for (Callback callback : current.getCallbacks()) {
                Fragment fragment = CallbackFragmentFactory.getInstance().getFragment(callback);
                getChildFragmentManager().beginTransaction()
                        .add(R.id.callbacks, fragment).commit();
            }
        }

        nextButton.setOnClickListener(v -> {
            errorLayout.setVisibility(View.INVISIBLE);
            authHandler.next(current);
        });

        //Action to proceed cancel
        cancelButton.setOnClickListener(v -> authHandler.cancel(new OperationCanceledException()));

        return view;
    }

    @Override
    public void onAuthenticationException(AuthenticationException e) {
        errorLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        authHandler = null;
    }

    @Override
    public void onDataCollected(Callback callback) {
        current.setCallback(callback);
    }

    @Override
    public void next() {
        authHandler.next(current);
    }
}
