/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.Pair;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import org.forgerock.android.auth.Config;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Collector to collect Display information
 */
public class DisplayCollector implements DeviceCollector {

    @Override
    public String getName() {
        return "display";
    }

    @Override
    public void collect(Context context, FRListener<JSONObject> listener) {
        try {
            Listener.onSuccess(listener, collect(context));
        } catch (JSONException e) {
            Listener.onException(listener, e);
        }
    }

    private JSONObject collect(Context context) throws JSONException {
        JSONObject o = new JSONObject();
        //o.put("displayId", Build.DISPLAY);
        Point point = getDisplayDimensions(context);
        o.put("width", point.x);
        o.put("height", point.y);
        o.put("orientation", getOrientation(context));
        return o;
    }

    private Integer getOrientation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            Display display = windowManager.getDefaultDisplay();
            if (display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180)
                return 1;
            else
                return 0;
        }
        return null;

    }

    private static Point getDisplayDimensions(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        return point;
    }


}
