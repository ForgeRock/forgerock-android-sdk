/*
 * Copyright (c) 2019 - 2020 ForgeRock. All rights reserved.
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.callback.Callback;
import org.forgerock.android.auth.exception.AuthenticationException;
import org.forgerock.android.auth.ui.AuthHandler;
import org.forgerock.android.auth.ui.AuthenticationExceptionListener;
import org.forgerock.android.auth.ui.CallbackFragmentFactory;
import org.forgerock.android.auth.ui.R;

import static android.text.TextUtils.isEmpty;
import static android.view.View.GONE;
import static org.forgerock.android.auth.ui.CallbackFragmentFactory.NODE;


/**
 * This Callback Fragment having the ability to change to suit different callback conditions.
 * The parent Fragment for all {@link CallbackFragment}
 */
public class AdaptiveCallbackFragment extends Fragment implements AuthenticationExceptionListener, CallbackController {

    private Node current;
    private LinearLayout errorLayout;
    private LinearLayout callbackLayout;
    private AuthHandler authHandler;
    private Button nextButton;
    private Button cancelButton;

    public AdaptiveCallbackFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            current = (Node) getArguments().getSerializable(NODE);
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
        callbackLayout = view.findViewById(R.id.callbacks);
        nextButton = view.findViewById(R.id.next);
        cancelButton = view.findViewById(R.id.cancel);

        TextView header = view.findViewById(R.id.header);
        if (isEmpty(current.getHeader())) {
            header.setVisibility(GONE);
        } else {
            header.setText(current.getHeader());
        }

        TextView description = view.findViewById(R.id.description);
        if (isEmpty(current.getDescription())) {
            description.setVisibility(GONE);
        } else {
            description.setText(current.getDescription());
        }

        //Add callback to LinearLayout Vertically
        if (savedInstanceState == null) {
            for (Callback callback : current.getCallbacks()) {
                Fragment fragment = CallbackFragmentFactory.getInstance().getFragment(current, callback);
                if (fragment != null) {
                    getChildFragmentManager().beginTransaction()
                            .add(R.id.callbacks, fragment).commit();
                }
            }
        }

        nextButton.setOnClickListener(v -> {
            errorLayout.setVisibility(View.INVISIBLE);
            authHandler.next(current);
        });

        //Action to proceed cancel
        cancelButton.setOnClickListener(v ->
                authHandler.cancel(new OperationCanceledException()));

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
    public void cancel(Exception e) {
        callbackLayout.setVisibility(GONE);
        authHandler.cancel(e);
    }

    @Override
    public void suspend() {
        nextButton.setVisibility(View.GONE);
    }

    @Override
    public void next() {
        callbackLayout.setVisibility(GONE);
        authHandler.next(current);
    }
}
