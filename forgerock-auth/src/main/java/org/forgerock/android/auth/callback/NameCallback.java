/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.json.JSONObject;

import lombok.NoArgsConstructor;

/**
 * Callback to collect a username
 */
public class NameCallback extends AbstractPromptCallback {


    /*
{
    "type": "NameCallback",
    "output": [
        {
            "name": "prompt",
            "value": "User Name"
        }
    ],
    "input": [
        {
            "name": "IDToken1",
            "value": ""
        }
    ]
}
 */

    @Keep
    public NameCallback() {
    }

    @Keep
    public NameCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    /**
     * Set the retrieved name.
     *
     * <p>
     *
     * @param name the retrieved name (which may be null).
     *
     */
    public void setName(String name) {
        setValue(name);
    }


    @Override
    public String getType() {
        return "NameCallback";
    }
}
