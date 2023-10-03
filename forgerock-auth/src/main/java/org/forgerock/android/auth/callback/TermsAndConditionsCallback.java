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

/**
 * A callback to collect a user's acceptance of the configured Terms &amp; Conditions.
 */
@Getter
public class TermsAndConditionsCallback extends AbstractCallback {

    /**
     * Retrieves the version.
     *
     * @return the version
     */
    private String version;

    /**
     * Retrieve the Terms &amp; Conditions.
     *
     * @return the Terms &amp; Conditions
     */
    private String terms;

    /**
     * The create date of the Terms &amp; Conditions.
     *
     * @return the create date.
     */
    private String createDate;

    @Keep
    public TermsAndConditionsCallback(JSONObject raw, int index) throws JSONException {
        super(raw, index);
    }

    @Keep
    public TermsAndConditionsCallback() {
    }

    @Override
    protected void setAttribute(String name, Object value) {
        switch (name) {
            case "version":
                this.version = (String) value;
                break;
            case "terms":
                this.terms = (String) value;
                break;
            case "createDate":
                this.createDate = (String) value;
                break;

            default:
                //ignore
        }
    }

    /**
     * Sets whether the Terms have been accepted.
     *
     * @param accept boolean representing the acceptance of the terms.
     */
    public void setAccept(boolean accept) {
        setValue(accept);
    }

    @Override
    public String getType() {
        return "TermsAndConditionsCallback";
    }
}
