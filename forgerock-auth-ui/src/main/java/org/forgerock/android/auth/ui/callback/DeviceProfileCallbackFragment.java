/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.callback.DeviceAttributeCallback;
import org.forgerock.android.auth.ui.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class DeviceProfileCallbackFragment extends  CallbackFragment<DeviceAttributeCallback> {


    public DeviceProfileCallbackFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_device_profile_callback, container, false);
        callback.execute(this.getContext(), new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                next();
            }

            @Override
            public void onException(Exception e) {
                //Do Something
            }
        });
        return view;
    }

}
