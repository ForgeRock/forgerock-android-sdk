/*
 * Copyright (c) 2019 - 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.json.JSONObject;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class HiddenValueCallback extends AbstractCallback {

    private String value;
    private String id;
    private String defaultValue = "";

    @Keep
    public HiddenValueCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        switch (name) {
            case "value":
                this.value = (String) value;
                break;
            case "id":
                this.id = (String) value;
                break;
            case "defaultValue":
                this.defaultValue = (String) value;
                break;
            default:
                //ignore
        }
    }

    public void setValue(String value) {
        super.setValue(value);
    }

    @Override
    public String getType() {
        return "HiddenValueCallback";
    }

    public String getValue() {
        return this.value;
    }

    public String getId() {
        return this.id;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }
}
