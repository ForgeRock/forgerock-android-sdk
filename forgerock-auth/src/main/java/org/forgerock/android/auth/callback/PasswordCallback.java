/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.json.JSONObject;

import java.util.Arrays;

import lombok.NoArgsConstructor;

/**
 * Callback to collect a username
 */
public class PasswordCallback extends AbstractPromptCallback {

    @Keep
    public PasswordCallback() {
    }

    @Keep
    public PasswordCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    /**
     * Set the retrieved password.
     *
     * <p>
     *
     * @param password the retrieved password, which may be null.
     *
     */
    public void setPassword(char[] password) {
        setValue(new String(password));
        Arrays.fill(password, ' ');
    }

    @Override
    public String getType() {
        return "PasswordCallback";
    }
}
