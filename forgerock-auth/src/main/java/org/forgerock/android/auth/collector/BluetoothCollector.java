/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Collector to collect device Bluetooth information
 */
public class BluetoothCollector implements DeviceCollector {

    private static final String TAG = BluetoothCollector.class.getSimpleName();

    @Override
    public String getName() {
        return "bluetooth";
    }

    @Override
    public void collect(Context context, FRListener<JSONObject> listener) {
        try {
            Listener.onSuccess(listener, collect(context));
        } catch (JSONException e) {
            Listener.onException(listener, e);
        }
    }

    public JSONObject collect(Context context) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("supported", isSupported(context));
        //o.put("macAddress", getMacAddress(context));
        return o;
    }

    private Boolean isSupported(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            return bluetoothManager.getAdapter() != null;
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    private String getMacAddress(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            BluetoothAdapter adapter = bluetoothManager.getAdapter();
            if (adapter != null) {
                try {
                    return adapter.getAddress();
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public void intercept(Chain chain, JSONObject data) {
        try {
            data.put(getName(), collect(chain.getContext()));
        } catch (JSONException e) {
            Logger.warn(TAG, e, "Failed to set data.");
        }
        chain.proceed(data);
    }
}
