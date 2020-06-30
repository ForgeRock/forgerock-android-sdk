/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.auth;

import android.content.Context;

import androidx.annotation.Keep;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.callback.DeviceProfileCallback;
import org.forgerock.android.auth.collector.DeviceCollector;
import org.forgerock.android.auth.collector.FRDeviceCollector;
import org.forgerock.android.auth.collector.HardwareCollector;
import org.forgerock.android.auth.collector.LocationCollector;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Sample to customize DeviceProfileCallback
 */
public class MyCustomDeviceProfile extends DeviceProfileCallback {

    public MyCustomDeviceProfile() {
    }

    @Keep
    public MyCustomDeviceProfile(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    public void execute(Context context, FRListener<Void> listener) {

        FRDeviceCollector.FRDeviceCollectorBuilder builder = FRDeviceCollector.builder();
        if (isMetadata()) {
            builder.collector(new DeviceCollector() {

                private final List<DeviceCollector> COLLECTORS = new ArrayList<>();

                @Override
                public String getName() {
                    return "metadata";
                }

                @Override
                public void collect(Context context, FRListener<JSONObject> listener) {
                    COLLECTORS.add(new HardwareCollector());
                    collect(context, listener, new JSONObject(), COLLECTORS);
                }
            });
        }
        if (isLocation()) {
            builder.collector(new LocationCollector());
        }

        builder.build().collect(context, new FRListener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                setValue(result.toString());
                Listener.onSuccess(listener, null);
            }

            @Override
            public void onException(Exception e) {
                Listener.onException(listener, null);
            }
        });
    }
}
