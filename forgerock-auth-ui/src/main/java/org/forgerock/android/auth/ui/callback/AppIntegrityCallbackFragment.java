/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;


import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.callback.AppIntegrityCallback;
import org.forgerock.android.auth.ui.R;

import static android.view.View.GONE;

/**
 * A simple {@link Fragment} subclass.
 */
public class AppIntegrityCallbackFragment extends CallbackFragment<AppIntegrityCallback> {

    private TextView message;
    private ProgressBar progressBar;

    public AppIntegrityCallbackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_app_integrity_callback, container, false);
        message = view.findViewById(R.id.message);
        progressBar = view.findViewById(R.id.appIntegrityApiCallProgress);

        if (node.getCallbacks().size() == 1) { //auto submit if there is one node
            progressBar.setVisibility(View.VISIBLE);
            message.setText("Performing " + callback.getRequestType() + " call...");
        } else {
            progressBar.setVisibility(GONE);
            message.setVisibility(GONE);
        }

        proceed();
        return view;
    }

    private void proceed() {
        final Activity thisActivity = (Activity) this.getActivity();
        callback.requestIntegrityToken(this.getContext(), new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                thisActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        message.setVisibility(GONE);
                        progressBar.setVisibility(GONE);
                        if (node.getCallbacks().size() == 1) { //auto submit if there is one node
                            next();
                        }
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                message.setVisibility(GONE);
                progressBar.setVisibility(GONE);
                Logger.error("AppIntegrityCallback", e.toString());
                cancel(e);
            }
        });
    }
}
