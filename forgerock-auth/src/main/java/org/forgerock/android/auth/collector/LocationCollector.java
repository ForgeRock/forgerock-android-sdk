/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Process;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * Collector to collect Last Known Location information, this collector will first try to use {@link FusedLocationProviderClient}
 * to collect last location data, if {@link FusedLocationProviderClient} class not found or failed to retrieve the location, it fallbacks to
 * Android framework Location API to retrieve the last known location. You may need to override this LocationCollector to collect more recent
 * location.
 */
public class LocationCollector implements DeviceCollector {

    @Override
    public String getName() {
        return "location";
    }

    @SuppressLint("MissingPermission")
    @Override
    public void collect(Context context, FRListener<JSONObject> listener) {

        if ((context.checkPermission(ACCESS_FINE_LOCATION, android.os.Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED) ||
                (context.checkPermission(ACCESS_COARSE_LOCATION, android.os.Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED)) {

            FusedLocationProviderClient client;
            try {
                client = LocationServices.getFusedLocationProviderClient(context);
            } catch (NoClassDefFoundError e) {
                fallback(context, listener);
                return;
            }
            client.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    JSONObject result = new JSONObject();
                    try {
                        result.put("latitude", location.getLatitude());
                        result.put("longitude", location.getLongitude());
                        Listener.onSuccess(listener, result);
                    } catch (JSONException e) {
                        fallback(context, listener);
                    }
                } else {
                    fallback(context, listener);
                }
            }).addOnFailureListener(e -> fallback(context, listener))
                    .addOnCanceledListener(() -> Listener.onSuccess(listener, null));

        } else {
            Listener.onSuccess(listener, null);
        }
    }

    /**
     * Fallback to use LocationManager
     *
     * @param context  The Application Context
     * @param listener Listener to listen for location collector event
     */
    @SuppressLint("MissingPermission")
    private void fallback(Context context, FRListener<JSONObject> listener) {
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {

            List<String> providers = locationManager.getProviders(true);
            Location bestLocation = null;
            for (String provider : providers) {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = l;
                }
            }
            Listener.onSuccess(listener, result(bestLocation));
        } else {
            Listener.onSuccess(listener, null);
        }
    }

    /**
     * Convert {@link Location} to {@link JSONObject}
     *
     * @param location The location
     * @return The JSONObject which represent the {@link Location}
     */
    protected JSONObject result(Location location) {

        if (location == null) return null;

        JSONObject result = new JSONObject();
        try {
            result.put("latitude", location.getLatitude());
            result.put("longitude", location.getLongitude());
            return result;
        } catch (JSONException e) {
            return null;
        }
    }
}
