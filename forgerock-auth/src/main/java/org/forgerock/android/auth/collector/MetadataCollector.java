/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.content.Context;

import org.forgerock.android.auth.FRListener;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Collector to collect Hardware information
 */
public class MetadataCollector implements DeviceCollector {

    private static final List<DeviceCollector> COLLECTORS = new ArrayList<>();

    static {
        COLLECTORS.add(new PlatformCollector());
        COLLECTORS.add(new HardwareCollector());
        COLLECTORS.add(new BrowserCollector());
        COLLECTORS.add(new BluetoothCollector());
        COLLECTORS.add(new NetworkCollector());
        COLLECTORS.add(new TelephonyCollector());
    }

    @Override
    public String getName() {
        return "metadata";
    }

    @Override
    public void collect(Context context, FRListener<JSONObject> listener) {
        collect(context, listener, new JSONObject(), COLLECTORS);
    }

}
