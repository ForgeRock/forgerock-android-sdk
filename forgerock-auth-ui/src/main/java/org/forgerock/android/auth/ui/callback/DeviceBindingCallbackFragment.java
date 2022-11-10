/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.callback.DeviceBindingCallback;
import org.forgerock.android.auth.ui.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class DeviceBindingCallbackFragment extends CallbackFragment<DeviceBindingCallback> {

    private TextView message;
    String TAG = DeviceBindingCallbackFragment.class.getSimpleName();

    public DeviceBindingCallbackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_device_binding_callback, container, false);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        proceed();
    }

    private void proceed() {
        callback.bind(this.getContext(), new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (node.getCallbacks().size() == 1) { //auto submit if there is one node
                    getActivity().runOnUiThread(() -> next());
                }
            }
            @Override
            public void onException(Exception e) {
                Log.e(TAG, e.getMessage());
                getActivity().runOnUiThread(() -> next());
            }
        });
    }
}
