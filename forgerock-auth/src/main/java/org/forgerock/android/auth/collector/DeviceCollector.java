/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.content.Context;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Interceptor;
import org.forgerock.android.auth.InterceptorHandler;
import org.forgerock.android.auth.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Collector to collect device information
 */
public interface DeviceCollector extends Interceptor<JSONObject> {

    String TAG = DeviceCollector.class.getSimpleName();

    /**
     * Retrieve the name of the Collector
     *
     * @return The name of the Collector
     */
    String getName();

    /**
     * Collect the device data.
     *
     * @param context  The Application Context
     * @param listener Listener to listen for collected data.
     */
    void collect(Context context, FRListener<JSONObject> listener);

    /**
     * Collect the device data with the provided {@link DeviceCollector}
     *
     * @param context    The application Context
     * @param listener   Listener to listen for collected data.
     * @param container  The container for the collected data.
     * @param collectors List of {@link DeviceCollector}
     */
    default void collect(Context context, FRListener<JSONObject> listener,
                         JSONObject container,
                         List<DeviceCollector> collectors) {
        InterceptorHandler interceptorHandler = InterceptorHandler.builder()
                .context(context)
                .interceptors(collectors)
                .listener(listener)
                .build();
        interceptorHandler.proceed(container);
    }


    @Override
    default void intercept(Chain chain, JSONObject data) {

        collect(chain.getContext(), new FRListener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                try {
                    data.put(getName(), result);
                } catch (JSONException e) {
                    Logger.warn(TAG, e, "Failed to set data");
                }
                chain.proceed(data);
            }

            @Override
            public void onException(Exception e) {
                //Continue to collect.
                chain.proceed(data);
            }
        });

    }
}
