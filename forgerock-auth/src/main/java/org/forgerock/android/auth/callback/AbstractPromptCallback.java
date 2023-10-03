/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.json.JSONObject;

import lombok.Getter;

/**
 * Callback that has prompt attribute
 */
@Getter
public abstract class AbstractPromptCallback extends AbstractCallback {

    /**
     * Get the prompt for this callback.
     *
     * @return this callback's prompt
     */
    public String prompt;

    public AbstractPromptCallback() {
    }

    public AbstractPromptCallback(JSONObject raw, int index) {
        super(raw, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        if ("prompt".equals(name)) {
            this.prompt = (String) value;
        }
    }
}
