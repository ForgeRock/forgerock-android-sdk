/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Callback to collect a password and validate it using IDM policy.
 */
@Getter
public class ValidatedPasswordCallback extends AbstractValidatedCallback {


    /**
     * Return whether the password
     * should be displayed as it is being typed.
     *
     * <p>
     *
     * @return the whether the password
     * should be displayed as it is being typed.
     */
    private boolean echoOn;

    private String prompt;

    @Keep
    public ValidatedPasswordCallback() {
    }

    @Keep
    public ValidatedPasswordCallback(@NotNull JSONObject raw, int index) throws JSONException {
        super(raw, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        switch (name) {
            case "prompt":
                this.prompt = (String) value;
                break;
            case "echoOn":
                this.echoOn = (boolean) value;
                break;
            default:
                //ignore
        }
    }

    /**
     * Set the retrieved password.
     *
     * @param password the retrieved password, which may be null.
     */
    public void setPassword(char[] password) {
        setValue(new String(password));
        Arrays.fill(password, ' ');
    }

    @Override
    public String getType() {
        return "ValidatedCreatePasswordCallback";
    }

}
