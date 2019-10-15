/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.detector.FRRootDetector;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.TimeZone;

/**
 * Collector to collect Platform information
 */
public class PlatformCollector implements DeviceCollector {

    @Override
    public String getName() {
        return "platform";
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
        o.put("platform", "Android");
        o.put("version", Build.VERSION.SDK_INT);
        o.put("device", Build.DEVICE);
        o.put("deviceName", Settings.Global.getString(context.getContentResolver(), "device_name"));
        o.put("model", Build.MODEL);
        o.put("brand", Build.BRAND);
        o.put("locale", getCurrentLocale(context));
        o.put("timeZone", TimeZone.getDefault().getID());
        o.put("jailBreakScore", isRooted(context));
        return o;
    }

    private double isRooted(Context context) {
        return FRRootDetector.DEFAULT.isRooted(context);
    }

    private Locale getCurrentLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return context.getResources().getConfiguration().locale;
        }
    }



}
