/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

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
        return o;
    }

    public static boolean isConnected(Context context) {
        if (context == null) return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        return true;
                    }
                }
            } else {
                try {
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                        return true;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }

        return false;
    }
}
