/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Collector to collect device Camera information
 */
public class CameraCollector implements DeviceCollector {

    @Override
    public String getName() {
        return "camera";
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
        o.put("numberOfCameras", getNumberOfCamera(context));
        return o;

    }

    private Integer getNumberOfCamera(Context context) {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            return manager.getCameraIdList().length;
        } catch (CameraAccessException e) {
            return null;
        }

    }

}
