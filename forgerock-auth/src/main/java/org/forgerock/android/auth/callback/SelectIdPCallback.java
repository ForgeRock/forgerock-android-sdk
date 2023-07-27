/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Callback to collect an Identity Provider
 */
public class SelectIdPCallback extends AbstractCallback {

    @Getter
    private List<IdPValue> providers;

    @Keep
    public SelectIdPCallback() {
    }

    @Keep
    public SelectIdPCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        switch (name) {
            case "providers":
                JSONArray array = (JSONArray) value;
                providers = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    try {
                        providers.add(new IdPValue(array.getJSONObject(i)));
                    } catch (Exception e) {
                        //ignore
                    }
                }
                break;
            default:
                //ignore
        }
    }

    @Override
    public String getType() {
        return "SelectIdPCallback";
    }

    public void setValue(String value) {
        super.setValue(value);
    }

    @Getter
    public static class IdPValue implements Serializable {

        //Keep it String for serializable
        private final String provider;
        private String uiConfig;

        public IdPValue(JSONObject jsonObject) {
            try {
                this.provider = jsonObject.getString("provider");
                JSONObject config = jsonObject.optJSONObject("uiConfig");
                if (config != null) {
                    this.uiConfig = config.toString();
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
