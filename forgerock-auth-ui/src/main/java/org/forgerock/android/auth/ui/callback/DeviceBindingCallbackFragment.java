/*
 * Copyright (c) 2020 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.ui.callback;


import static android.view.View.GONE;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.callback.DeviceBindingCallback;
import org.forgerock.android.auth.devicebind.Abort;
import org.forgerock.android.auth.devicebind.DeviceBindingException;
import org.forgerock.android.auth.devicebind.Timeout;
import org.forgerock.android.auth.devicebind.Unsupported;
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
        message = view.findViewById(R.id.message);
        if (node.getCallbacks().size() == 1) { //auto submit if there is one node
            message.setText(callback.getContent());
        } else {
            message.setVisibility(GONE);
        }

        proceed();
        return view;
    }

    private void proceed() {
        callback.bind(this.getContext(), new FRListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                message.setVisibility(GONE);
                if (node.getCallbacks().size() == 1) { //auto submit if there is one node
                    next();
                }
            }
            @Override
            public void onException(Exception e) {
                if (e instanceof DeviceBindingException) {
                    Log.e(TAG, e.getMessage());

                    if(((DeviceBindingException) e).getStatus() instanceof Timeout) {
                        callback.setClientError("Timeout");
                    }
                    else if(((DeviceBindingException) e).getStatus() instanceof Abort) {
                        callback.setClientError("Abort");
                    }
                    else if(((DeviceBindingException) e).getStatus() instanceof Unsupported) {
                        callback.setClientError("Unsupported");
                    }
                    next();
                }
                else {
                    message.setText(e.getMessage());
                }
            }
        });
    }
}
