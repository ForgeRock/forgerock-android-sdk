/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;


import static android.view.View.GONE;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.callback.DeviceBindingCallback;
import org.forgerock.android.auth.ui.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class DeviceBindingCallbackFragment extends CallbackFragment<DeviceBindingCallback> {

    private TextView message;
    private ProgressBar progressBar;

    public DeviceBindingCallbackFragment() {
        // Required empty public constructor
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_device_enrollment_callback, container, false);
        message = view.findViewById(R.id.message);
        progressBar = view.findViewById(R.id.deviceEnrollmentProgress);
        proceed();

        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void proceed() {
        callback.bind(this.getContext(), new FRListener<Void>() {
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
                next();
                //cancel(e);
            }
        });
    }
}
