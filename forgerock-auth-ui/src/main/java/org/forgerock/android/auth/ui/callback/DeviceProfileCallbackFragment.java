/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;


import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.callback.DeviceProfileCallback;
import org.forgerock.android.auth.ui.R;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.view.View.GONE;

/**
 * A simple {@link Fragment} subclass.
 */
public class DeviceProfileCallbackFragment extends CallbackFragment<DeviceProfileCallback> {

    private TextView message;
    private ProgressBar progressBar;
    public static final int LOCATION_REQUEST_CODE = 100;

    public DeviceProfileCallbackFragment() {
        // Required empty public constructor
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //No matter permission is granted or not, we proceed to the next node.
        proceed();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_device_profile_callback, container, false);
        message = view.findViewById(R.id.message);
        progressBar = view.findViewById(R.id.collectingDeviceProfileProgress);
        if (node.getCallbacks().size() == 1) { //auto submit if there is one node
            progressBar.setVisibility(View.VISIBLE);
            message.setText(callback.getMessage());
        } else {
            progressBar.setVisibility(GONE);
            message.setVisibility(GONE);
        }

        if (callback.isLocation()) {
            if (ContextCompat.checkSelfPermission(getContext(), ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                //We don't have permission.
                if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                    AlertDialog alertDialog = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme)
                            .setMessage(R.string.request_location_rationale)
                            .setPositiveButton("Proceed",
                                    (dialog, which) -> requestLocationPermission())
                            .setNegativeButton("Deny", (dialog, which) -> proceed())
                            .create();
                    alertDialog.show();
                } else {
                    requestLocationPermission();
                }
            } else {
                // Location permission is granted, proceed.
                proceed();
            }
        } else {
            proceed();
        }

       return view;
    }

    private void proceed() {
        callback.execute(this.getContext(), new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                message.setVisibility(GONE);
                progressBar.setVisibility(GONE);
                if (node.getCallbacks().size() == 1) { //auto submit if there is one node
                    next();
                }
            }

            @Override
            public void onException(Exception e) {
                message.setVisibility(GONE);
                progressBar.setVisibility(GONE);
                //Not likely to happen, Device Collector try best to collect.
                cancel(e);
            }
        });
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{ACCESS_FINE_LOCATION}
                , LOCATION_REQUEST_CODE);
    }
}
