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

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback;
import org.forgerock.android.auth.ui.R;

import static android.view.View.GONE;

/**
 * UI representation for {@link WebAuthnRegistrationCallback}
 */
public class WebAuthnRegistrationCallbackFragment extends CallbackFragment<WebAuthnRegistrationCallback> {

    private ProgressBar progressBar;

    /**
     * Default constructor for WebAuthnRegistrationCallbackFragment
     */
    public WebAuthnRegistrationCallbackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_webauthn_registration_callback, container, false);
        progressBar = view.findViewById(R.id.progress);

        callback.register(this, node, new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                progressBar.setVisibility(GONE);
                next();
            }

            @Override
            public void onException(Exception e) {
                progressBar.setVisibility(GONE);
                next();
            }
        });

        return view;
    }

}
