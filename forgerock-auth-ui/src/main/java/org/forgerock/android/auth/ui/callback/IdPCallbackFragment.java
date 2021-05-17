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
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.callback.IdPCallback;
import org.forgerock.android.auth.ui.R;

import static android.view.View.GONE;

/**
 * A simple {@link Fragment} subclass.
 */
public class IdPCallbackFragment extends CallbackFragment<IdPCallback> {

    private TextView message;
    private ProgressBar progressBar;

    public IdPCallbackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_idp_callback, container, false);
        message = view.findViewById(R.id.message);
        message.setText("redirecting...");
        progressBar = view.findViewById(R.id.loading);
        progressBar.setVisibility(View.VISIBLE);
        proceed();

        return view;
    }

    private void proceed() {
        callback.signIn(this, null, new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                message.setVisibility(GONE);
                progressBar.setVisibility(GONE);
                next();
            }

            @Override
            public void onException(Exception e) {
                message.setVisibility(GONE);
                progressBar.setVisibility(GONE);
                cancel(e);
            }
        });
    }

}
