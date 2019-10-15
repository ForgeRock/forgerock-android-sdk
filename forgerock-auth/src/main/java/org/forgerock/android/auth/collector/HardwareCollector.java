/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import org.forgerock.android.auth.FRListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * Collector to collect Hardware information
 */
public class HardwareCollector implements DeviceCollector {

    private static final List<DeviceCollector> COLLECTORS = new ArrayList<>();

    static {
        COLLECTORS.add(new DisplayCollector());
        COLLECTORS.add(new CameraCollector());
    }

    @Override
    public String getName() {
        return "hardware";
    }

    @Override
    public void collect(Context context, FRListener<JSONObject> listener) {
        try {
            collect(context, listener, collect(context), COLLECTORS);
        } catch (JSONException e) {
            collect(context, listener, new JSONObject(), COLLECTORS);
        }
    }

    public JSONObject collect(Context context) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("hardware", Build.HARDWARE);
        o.put("manufacturer", Build.MANUFACTURER);
        o.put("storage", getStorage());
        o.put("memory", getRam(context));
        o.put("cpu", Runtime.getRuntime().availableProcessors());
        return o;
    }

    private JSONArray getJSONArray(String[] array) {
        JSONArray result = new JSONArray();
        for (String s : array) {
            result.put(s);
        }
        return result;
    }



    private Long getStorage() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return totalBlocks * blockSize / (1024 * 1024);
    }

    private Long getRam(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.getMemoryInfo(mi);
            return mi.totalMem / 1048576L;
        }
        return null;
    }
}
