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
import org.forgerock.android.auth.callback.ConfirmationCallback;
import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback;
import org.forgerock.android.auth.ui.R;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_webauthn_authentication_callback, container, false);
        progressBar = view.findViewById(R.id.progress);

        callback.authenticate(this, node, null, new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                progressBar.setVisibility(View.GONE);
                next();
            }

            @Override
            public void onException(Exception e) {
                progressBar.setVisibility(View.GONE);
                //When there recovery code is enable to don't proceed the authentication.
                if (node.getCallback(ConfirmationCallback.class) == null) {
                    next();
                }
            }
        });

        return view;
    }

}
