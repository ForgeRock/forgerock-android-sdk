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
 * Callback to display information messages,
 * warning messages and error messages.
 */
@Getter
public class TextOutputCallback extends AbstractCallback {

    //Message Type
    /**
     * Information message.
     */
    public static final int INFORMATION = 0;
    /**
     * Warning message.
     */
    public static final int WARNING = 1;
    /**
     * Error message.
     */
    public static final int ERROR = 2;

    /**
     * The message type
     */
    private int messageType;

    /**
     * The message
     */
    private String message;

    @Keep
    public TextOutputCallback(JSONObject raw, int index) {
        super(raw, index);
    }

    @Keep
    public TextOutputCallback() {
    }

    @Override
    protected void setAttribute(String name, Object value) {
        switch (name) {
            case "messageType":
                this.messageType = Integer.parseInt((String) value);
                break;
            case "message":
                this.message = (String) value;
                break;
            default:
                //ignore
        }
    }

    @Override
    public String getType() {
        return "TextOutputCallback";
    }

}
