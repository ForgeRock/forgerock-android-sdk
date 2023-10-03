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
public class NumberAttributeInputCallback extends AttributeInputCallback {

    /**
     * The attribute Value
     */
    private Double value;

    @Keep
    public NumberAttributeInputCallback() {
    }

    /**
     * Constructor for this Callback.
     */
    @Keep
    public NumberAttributeInputCallback(JSONObject jsonObject, int index) throws JSONException {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        if (VALUE.equals(name)) {
            if (value instanceof Integer) {
                this.value = ((Integer)value).doubleValue();
            } else {
                this.value = (Double) value;
            }
        }
    }

    public void setValue(Double value) {
        super.setValue(value);
    }

    @Override
    public String getType() {
        return "NumberAttributeInputCallback";
    }
}
