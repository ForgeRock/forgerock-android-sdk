/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Callback to collect user's Consent.
 */
@Getter
public class ConsentMappingCallback extends AbstractCallback {

    private String name;
    private String displayName;
    private String icon;
    private String accessLevel;
    private boolean isRequired;
    private String[] fields;
    private String message;

    @Keep
    public ConsentMappingCallback() {
    }

    @Keep
    public ConsentMappingCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        switch (name) {
            case "name":
                this.name = (String) value;
                break;
            case "displayName":
                this.displayName = (String) value;
                break;
            case "icon":
                this.icon = (String) value;
                break;
            case "accessLevel":
                this.accessLevel = (String) value;
                break;
            case "isRequired":
                this.isRequired = (boolean) value;
                break;
            case "fields":
                JSONArray array = (JSONArray) value;
                if (array != null) {
                    this.fields = new String[array.length()];
                    for (int i = 0; i < array.length(); i++) {
                        this.fields[i] = array.optString(i);
                    }
                }
                break;
            case "message":
                this.message = (String) value;
                break;
            default:
                //ignore
        }

    }

    /**
     * Sets whether the Consent has been accepted.
     *
     * @param accept boolean representing the acceptance of the consent.
     */
    public void setAccept(boolean accept) {
        setValue(accept);
    }

    @Override
    public String getType() {
        return "ConsentMappingCallback";
    }

}
