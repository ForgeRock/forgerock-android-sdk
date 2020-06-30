/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.content.Context;

import org.forgerock.android.auth.Config;
import org.forgerock.android.auth.DeviceIdentifier;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * Collector to collect Device information
 */
@Builder
public class FRDeviceCollector implements DeviceCollector {

    public static final List<DeviceCollector> DEFAULT_COLLECTORS = new ArrayList<>();

    static {
        DEFAULT_COLLECTORS.add(new PlatformCollector());
        DEFAULT_COLLECTORS.add(new HardwareCollector());
        DEFAULT_COLLECTORS.add(new BrowserCollector());
        DEFAULT_COLLECTORS.add(new BluetoothCollector());
        DEFAULT_COLLECTORS.add(new NetworkCollector());
        DEFAULT_COLLECTORS.add(new TelephonyCollector());
        DEFAULT_COLLECTORS.add(new LocationCollector());
    }

    /**
     * The Default Device Collector to retrieve the device attribute.
     */
    public static final FRDeviceCollector DEFAULT = FRDeviceCollector.builder()
            .collectors(DEFAULT_COLLECTORS).build();

    @Singular
    @Getter
    private List<DeviceCollector> collectors;

    @Override
    public String getName() {
        return null;
    }


    private JSONObject collect(Context context) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("identifier", DeviceIdentifier.builder().context(context)
                .keyStoreManager(Config.getInstance().getKeyStoreManager())
                .build().getIdentifier());
        result.put("version", "1.0");
        return result;
    }

    @Override
    public void collect(Context context, FRListener<JSONObject> listener) {

        try {
            collect(context, listener, collect(context), collectors);
        } catch (JSONException e) {
            Listener.onException(listener, e);
        }
    }
}
