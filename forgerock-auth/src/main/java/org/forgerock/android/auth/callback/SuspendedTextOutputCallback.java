/*
 * Copyright (c) 2020 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.json.JSONObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Callback to display information messages,
 * warning messages and error messages.
 */
@Getter
public class SuspendedTextOutputCallback extends TextOutputCallback {

    @Keep
    public SuspendedTextOutputCallback() {
    }

    @Keep
    public SuspendedTextOutputCallback(JSONObject raw, int index) {
        super(raw, index);
    }


    @Override
    public String getType() {
        return "SuspendedTextOutputCallback";
    }

}
