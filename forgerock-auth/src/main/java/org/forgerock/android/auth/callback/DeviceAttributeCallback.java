/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Keep;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.forgerock.android.auth.collector.FRDeviceCollector;
import org.forgerock.android.auth.collector.FRDeviceCollector.FRDeviceCollectorBuilder;
import org.forgerock.android.auth.collector.LocationCollector;
import org.forgerock.android.auth.collector.ProfileCollector;
import org.json.JSONObject;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class DeviceAttributeCallback extends HiddenValueCallback implements ActionCallback {

    private static final String PROFILE = "profile";
    private static final String LOCATION = "location";
    /**
     * Attributes to collect
     */
    private List<String> attributes;

    @Keep
    public DeviceAttributeCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
        Uri uri = Uri.parse(getId());
        attributes = uri.getQueryParameters("attributes");
    }

    @Override
    public void execute(Context context, FRListener<Void> listener) {

        FRDeviceCollectorBuilder builder = FRDeviceCollector.builder();
        for (String attribute: attributes) {
            switch (attribute) {
                case PROFILE:
                    builder.collector(new ProfileCollector());
                    break;
                case LOCATION:
                    builder.collector(new LocationCollector());
                    break;
            }
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

    @Override
    public String getType() {
        return "DeviceAttributeCallback";
    }

}
