/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.content.Context;
import android.webkit.WebSettings;

import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Collector to collect device Browser information
 */
public class BrowserCollector implements DeviceCollector {

    private static final String USER_AGENT = "userAgent";

    @Override
    public String getName() {
        return "browser";
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
        try {

            o.put(USER_AGENT, WebSettings.getDefaultUserAgent(context));
        } catch (Exception e) {
            //fallback to http agent
            o.put(USER_AGENT, System.getProperty("http.agent"));
        }
        return o;
    }
}
