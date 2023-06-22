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
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback;
import org.forgerock.android.auth.ui.R;
import org.forgerock.android.auth.webauthn.WebAuthnKeySelector;

import static android.view.View.GONE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement;

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        callback.setResidentKeyRequirement(ResidentKeyRequirement.RESIDENT_KEY_DISCOURAGED);
        callback.register(requireContext(), node, new FRListener<Void>() {
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_webauthn_registration_callback, container, false);
        progressBar = view.findViewById(R.id.progress);
        return view;
    }

}