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
import lombok.NoArgsConstructor;

/**
 * Class representative of a PollingWaitCallback Callback Object which instructs a client to wait for the given period
 * and then resubmit their request.
 */
@Getter
public class PollingWaitCallback extends AbstractCallback {

    /**
     * The period of time in milliseconds that the client should wait before replying to this callback.
     */
    private String waitTime;
    /**
     * The message which should be displayed to the user
     */
    private String message;

    @Keep
    public PollingWaitCallback() {
    }

    /**
     * Constructor for creating this Callback
     */
    @Keep
    public PollingWaitCallback(JSONObject raw, int index) {
        super(raw, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        switch (name) {
            case "waitTime":
                this.waitTime = (String) value;
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
        return "PollingWaitCallback";
    }
}
