/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import android.content.Context;

import androidx.annotation.Keep;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.collector.FRDeviceCollector;
import org.forgerock.android.auth.collector.FRDeviceCollector.FRDeviceCollectorBuilder;
import org.forgerock.android.auth.collector.LocationCollector;
import org.forgerock.android.auth.collector.MetadataCollector;
import org.json.JSONObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class DeviceProfileCallback extends AbstractCallback implements ActionCallback {

    /**
     * Request the SDK to capture device metadata.
     */
    private boolean metadata;

    /**
     * Request the SDK to capture device location.
     */
    private boolean location;

    /**
     * The message which should be displayed to the user
     */
    private String message;

    @Keep
    public DeviceProfileCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        switch (name) {
            case "metadata":
                this.metadata = (Boolean) value;
                break;
            case "location":
                this.location = (Boolean) value;
                break;
            case "message":
                this.message = (String) value;
                break;
            default:
                //ignore
        }
    }

    public void setValue(String value) {
        super.setValue(value);
    }

    @Override
    public void execute(Context context, FRListener<Void> listener) {

        FRDeviceCollectorBuilder builder = FRDeviceCollector.builder();
        if (metadata) {
            builder.collector(new MetadataCollector());
        }
        if (location) {
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
                Listener.onException(listener, e);
            }
        });
    }

    @Override
    public String getType() {
        return "DeviceProfileCallback";
    }

}
