/*
 * Copyright (c) 2021 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import static org.forgerock.android.auth.callback.AbstractProtectCallbackKt.PING_ONE_PROTECT_EVALUATION_CALLBACK;
import static org.forgerock.android.auth.callback.AbstractProtectCallbackKt.PING_ONE_PROTECT_INITIALIZE_CALLBACK;

import androidx.annotation.Keep;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A callback that allows some extra metadata to be sent in the response.
 * <p>
 * When serialized as JSON in an authenticate response, the {@link #value} object will be the value of a single
 * {@literal data} output value, so for a value of {@code { "foo": "bar" }}, this would be output as:
 * <pre>
 * {@code
 * {
 *     "authId": "...",
 *     "callbacks": [
 *         // ...
 *         {
 *             "type": "MetadataCallback",
 *             "output": [
 *                 {
 *                     "name": "data",
 *                     "value": {
 *                         "foo": "bar"
 *                     }
 *                 }
 *             ]
 *         }
 *     ]
 * }
 * }
 * </pre>
 */
public class MetadataCallback extends AbstractCallback implements DerivableCallback {

    private String value;

    @Keep
    public MetadataCallback() {
    }

    public JSONObject getValue() {
        if (value != null) {
            try {
                return new JSONObject(value);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Keep
    public MetadataCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        if ("data".equals(name)) {
            if (value != null) {
                this.value = value.toString();
            }
        }
    }

    @Override
    public String getType() {
        return "MetadataCallback";
    }

    @Override
    public Class<? extends Callback> getDerivedCallback() {
        JSONObject value = getValue();
        if (WebAuthnRegistrationCallback.instanceOf(value)) {
            return CallbackFactory.getInstance().getCallbacks().get("WebAuthnRegistrationCallback");
        }
        if (WebAuthnAuthenticationCallback.instanceOf(value)) {
            return CallbackFactory.getInstance().getCallbacks().get("WebAuthnAuthenticationCallback");
        }
        if (AbstractProtectCallback.isPingOneProtectInitializeCallback(value)) {
            return CallbackFactory.getInstance().getCallbacks().get(PING_ONE_PROTECT_INITIALIZE_CALLBACK);
        }
        if (AbstractProtectCallback.isPingOneProtectEvaluationCallback(value)) {
            return CallbackFactory.getInstance().getCallbacks().get(PING_ONE_PROTECT_EVALUATION_CALLBACK);
        }
        return null;
    }
}
