/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Implements a Callback for collection of a single identity object attribute from a user.
 */
@Getter
public class StringAttributeInputCallback extends AttributeInputCallback {

    /**
     * The attribute Value
     */
    private String value;

    @Keep
    public StringAttributeInputCallback() {
    }

    /**
     * Constructor for this Callback.
     */
    @Keep
    public StringAttributeInputCallback(JSONObject jsonObject, int index) throws JSONException {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        if (VALUE.equals(name)) {
            this.value = (String) value;
        }
    }

    public void setValue(String value) {
        super.setValue(value);
    }

    @Override
    public String getType() {
        return "StringAttributeInputCallback";
    }
}
