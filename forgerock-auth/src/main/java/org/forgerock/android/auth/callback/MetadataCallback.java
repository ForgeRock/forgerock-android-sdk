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
@NoArgsConstructor
@Getter
public class MetadataCallback extends AbstractCallback {

    private JSONObject value;

    @Keep
    public MetadataCallback(JSONObject jsonObject, int index) {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        if ("data".equals(name)) {
            this.value = (JSONObject) value;
        }
    }

    @Override
    public String getType() {
        return "MetadataCallback";
    }
}
