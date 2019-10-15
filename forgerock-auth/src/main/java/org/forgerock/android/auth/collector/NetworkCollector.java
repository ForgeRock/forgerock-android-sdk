/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Collector to collect Network information
 */
public class NetworkCollector implements DeviceCollector {

    @Override
    public String getName() {
        return "network";
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
        o.put("connected", isConnected(context));
        //o.put("macAddress", getMacAddress(context));
        return o;
    }

    private Boolean isConnected(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            try {
                return wifiManager.isWifiEnabled();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String getMacAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            try {
                return wifiManager.getConnectionInfo().getMacAddress();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }



}
