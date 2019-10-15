/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Base implementation of a Callback for collection of a single identity object attribute from a user.
 */
@NoArgsConstructor
@Getter
public abstract class AttributeInputCallback extends AbstractValidatedCallback {

    private String prompt;
    private String name;
    private boolean required;

    public AttributeInputCallback(JSONObject jsonObject, int index) throws JSONException {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        switch (name) {
            case "name":
                this.name = (String) value;
                break;
            case "prompt":
                this.prompt = (String) value;
                break;
            case "required":
                this.required = (boolean) value;
                break;
            default:
                //ignore
        }

    }
}
