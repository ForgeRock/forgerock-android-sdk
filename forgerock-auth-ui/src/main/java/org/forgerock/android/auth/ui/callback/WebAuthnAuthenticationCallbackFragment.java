/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.callback.ConfirmationCallback;
import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback;
import org.forgerock.android.auth.ui.R;
import org.forgerock.android.auth.webauthn.WebAuthnKeySelector;

/**
 * UI representation for {@link WebAuthnAuthenticationCallback}
 */
public class WebAuthnAuthenticationCallbackFragment extends CallbackFragment<WebAuthnAuthenticationCallback> {
    private ProgressBar progressBar;

    /**
     * Default constructor for WebAuthnAuthenticationCallbackFragment
     */
    public WebAuthnAuthenticationCallbackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_webauthn_authentication_callback, container, false);
        progressBar = view.findViewById(R.id.progress);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        callback.authenticate(requireContext(), node,  WebAuthnKeySelector.DEFAULT, new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                progressBar.setVisibility(View.GONE);
                next();
            }

            @Override
            public void onException(Exception e) {
                progressBar.setVisibility(View.GONE);
                if (node.getCallback(ConfirmationCallback.class) == null) {
                    next();
                }
            }
        });
    }
}